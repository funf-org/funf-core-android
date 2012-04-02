package edu.mit.media.funf.probe.builtin;

import java.lang.reflect.Type;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AccountsKeys;

@RequiredPermissions(android.Manifest.permission.GET_ACCOUNTS)
public class AccountsProbe extends ImpulseProbe implements AccountsKeys {

	@Override
	protected GsonBuilder getGsonBuilder() {
		GsonBuilder builder = super.getGsonBuilder();
		builder.registerTypeAdapter(Account.class, new JsonSerializer<Account>() {
			@Override
			public JsonElement serialize(Account src, Type typeOfSrc, JsonSerializationContext context) {
				JsonObject account = new JsonObject();
				account.addProperty(NAME, sensitiveData(src.name));
				account.addProperty(TYPE, src.type);
				return account;
			}
		});
		return builder;
	}

	@Override
	public void onStart() {
		AccountManager am = (AccountManager)getContext().getSystemService(Context.ACCOUNT_SERVICE);
		Gson gson = getGson();
		for (Account account : am.getAccounts()) {
			sendData(gson.toJsonTree(account).getAsJsonObject());
		}
		disable();
	}
}
