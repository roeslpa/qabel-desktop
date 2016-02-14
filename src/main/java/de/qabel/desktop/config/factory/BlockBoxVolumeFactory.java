package de.qabel.desktop.config.factory;

import de.qabel.core.accounting.AccountingHTTP;
import de.qabel.core.config.Account;
import de.qabel.core.config.Identity;
import de.qabel.desktop.daemon.management.MagicEvilPrefixSource;
import de.qabel.desktop.storage.BlockReadBackend;
import de.qabel.desktop.storage.BlockWriteBackend;
import de.qabel.desktop.storage.BoxVolume;
import de.qabel.desktop.storage.cache.CachedBoxVolume;

import java.io.File;

public class BlockBoxVolumeFactory implements BoxVolumeFactory {
	private byte[] deviceId;
	private AccountingHTTP accountingHTTP;

	public BlockBoxVolumeFactory(byte[] deviceId, AccountingHTTP accountingHTTP) {
		this.deviceId = deviceId;
		this.accountingHTTP = accountingHTTP;
	}

	@Override
	public BoxVolume getVolume(Account account, Identity identity) {
		String prefix = MagicEvilPrefixSource.getPrefix(account);

		String root = "http://localhost:9697/api/v0/files/" + prefix;

		BlockReadBackend readBackend = new BlockReadBackend(root, accountingHTTP);
		BlockWriteBackend writeBackend = new BlockWriteBackend(root, accountingHTTP);

		return new CachedBoxVolume(
				readBackend,
				writeBackend,
				identity.getPrimaryKeyPair(),
				deviceId,
				new File(System.getProperty("java.io.tmpdir")),
				prefix
		);
	}
}
