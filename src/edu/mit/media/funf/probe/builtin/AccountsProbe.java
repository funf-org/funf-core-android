package edu.mit.media.funf.probe.builtin;

import java.util.ArrayList;
import java.util.Arrays;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import edu.mit.media.funf.HashUtil;
import edu.mit.media.funf.probe.SynchronousProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AccountsKeys;

public class AccountsProbe extends SynchronousProbe implements AccountsKeys {

	@Override
	protected Bundle getData() {
		AccountManager am = (AccountManager)this.getApplicationContext().getSystemService(ACCOUNT_SERVICE);
		ArrayList<Bundle> accountBundles = new ArrayList<Bundle>();
		for (Account account : am.getAccounts()) {
			Bundle accountBundle = new Bundle();
			accountBundle.putString(NAME, HashUtil.hashString(this, account.name));
			accountBundle.putString(TYPE, account.type);
			accountBundles.add(accountBundle);
		}
		Bundle data = new Bundle();
		data.putParcelableArrayList(ACCOUNTS, accountBundles);
		return data;
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
				android.Manifest.permission.GET_ACCOUNTS
		};
	}

}
