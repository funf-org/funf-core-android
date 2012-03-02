package edu.mit.media.funf.probe.builtin;

import java.lang.reflect.Type;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.Probe.StartableProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AccountsKeys;

@RequiredPermissions(android.Manifest.permission.GET_ACCOUNTS)
public class AccountsProbe extends SimpleProbe<Account> implements StartableProbe, AccountsKeys {

	@Override
	protected JsonSerializer<Account> getSerializer() {
		return new JsonSerializer<Account>() {
			@Override
			public JsonElement serialize(Account src, Type typeOfSrc, JsonSerializationContext context) {
				JsonObject account = new JsonObject();
				account.addProperty(NAME, sensitiveData(src.name));
				account.addProperty(TYPE, src.type);
				return account;
			}
		};
	}

	@Override
	public void onStart() {
		AccountManager am = (AccountManager)getContext().getSystemService(Context.ACCOUNT_SERVICE);
		for (Account account : am.getAccounts()) {
			sendData(account);
		}
		disablePassive();
	}
}
