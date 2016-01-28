package de.qabel.desktop.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Entity;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropInvalidURL;
import de.qabel.desktop.ui.accounting.GsonContact;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class AbstractController {
	protected Alert alert;
	protected Label exceptionLabel;
	protected Gson gson;

	protected void alert(String message, Exception e) {
		Logger.getLogger(getClass().getSimpleName()).error(message, e);

		alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(message);

		TextArea textArea = new TextArea(getTraceAsString(e));
		VBox.setMargin(textArea, new Insets(10, 0, 5, 0));
		textArea.setEditable(false);
		textArea.setWrapText(false);

		VBox.setVgrow(textArea, Priority.ALWAYS);

		exceptionLabel = new Label(e.getMessage());
		VBox expansion = new VBox();
		expansion.getChildren().add(exceptionLabel);
		expansion.getChildren().add(textArea);

		alert.getDialogPane().setContent(expansion);
		alert.setResizable(true);
		alert.showAndWait();
	}

	private String getTraceAsString(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}

	protected Function<String, Object> singleObjectMap(String key, Object instance) {
		return s -> {
			if (s.equals(key)) {
				return instance;
			}
			return null;
		};
	}

	protected GsonContact createGsonFromEntity(Entity entity) {
		GsonContact gc = new GsonContact();

		if (entity instanceof Contact) {
			Contact c = (Contact) entity;
			gc.setEmail(c.getEmail());
			gc.setPhone(c.getPhone());
			gc.setAlias(c.getAlias());
		} else {
			Identity i = (Identity) entity;
			gc.setEmail(i.getEmail());
			gc.setPhone(i.getPhone());
			gc.setAlias(i.getAlias());
		}

		gc.setCreated(entity.getCreated());
		gc.setUpdated(entity.getUpdated());
		gc.setDeleted(entity.getDeleted());
		gc.setPublicKey(entity.getEcPublicKey().getKey());
		for (DropURL d : entity.getDropUrls()) {
			gc.addDropUrl(d.getUri().toString());
		}
		return gc;
	}


	protected void writeStringInFile(String json, File dir) throws IOException {
		File targetFile = new File(dir.getPath());
		targetFile.createNewFile();
		OutputStream outStream = new FileOutputStream(targetFile);
		outStream.write(json.getBytes());
	}

	protected void buildGson() {
		final GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		builder.excludeFieldsWithoutExposeAnnotation();
		gson = builder.create();
	}

	public String readFile(File f) throws IOException {
		FileReader fileReader = new FileReader(f);
		BufferedReader br = new BufferedReader(fileReader);

		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				line = br.readLine();
				if (line != null) {
					sb.append("\n");
				}
			}
			return sb.toString();
		} finally {
			br.close();
		}
	}

	public Contact gsonContactToContact(GsonContact gc, Identity i) throws URISyntaxException, QblDropInvalidURL {

		ArrayList<DropURL> collection = generateDropURLs(gc.getDropUrls());
		QblECPublicKey pubKey = new QblECPublicKey(gc.getPublicKey());
		Contact c = new Contact(i, gc.getAlias(), collection, pubKey);
		c.setPhone(gc.getPhone());
		c.setEmail(gc.getEmail());

		return c;
	}

	private ArrayList<DropURL> generateDropURLs(List<String> drops) throws URISyntaxException, QblDropInvalidURL {
		ArrayList<DropURL> collection = new ArrayList<>();

		for (String uri : drops) {
			DropURL dropURL = new DropURL(uri);

			collection.add(dropURL);
		}
		return collection;
	}
	protected String calculateTimeString(DropMessage dropMessage) {

		Long messageDate = dropMessage.getCreationDate().getTime();

		java.util.Date date= new java.util.Date();
		Timestamp currentTimestamp = new Timestamp(date.getTime());

		long diff = currentTimestamp.getTime() - messageDate;

		int minutes = (int) ((diff / (1000*60)));
		int hours   = (int) ((diff / (1000*60*60)));

		String text;
		if (minutes < 10) {
			text = "moments ago";
		} else if (minutes < 60) {
			text = String.valueOf(minutes + " minutes ago");
		} else if (hours < 24) {
			text = String.valueOf(hours + " hours ago");
		} else {
			text = dropMessage.getCreationDate().toString();
		}

		return text;
	}
}
