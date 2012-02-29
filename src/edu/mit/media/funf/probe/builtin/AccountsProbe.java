package edu.mit.media.funf.probe.builtin;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.mit.media.funf.HashUtil;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.Probe.StartableProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AccountsKeys;

@RequiredPermissions(android.Manifest.permission.GET_ACCOUNTS)
public class AccountsProbe extends Base implements StartableProbe, AccountsKeys {

	@Override
	public void onStart() {
		AccountManager am = (AccountManager)getContext().getSystemService(Context.ACCOUNT_SERVICE);
		JsonArray accounts = new JsonArray();
		for (Account account : am.getAccounts()) {
			JsonObject accountObject = new JsonObject();
			accountObject.addProperty(NAME, HashUtil.hashString(getContext(), account.name));
			accountObject.addProperty(TYPE, account.type);
			accounts.add(accountObject);
		}
		
		JsonObject data = new JsonObject();
		data.add(ACCOUNTS, accounts);
		sendData(data);
		
		disable();
	}
}
