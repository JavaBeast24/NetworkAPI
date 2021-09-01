package com.beastdevelopment.javabeast.networkapi;

import com.beastdevelopment.javabeast.networkapi.defaultpacks.PingPack;

import java.util.HashMap;

public abstract class Package {

    public static final HashMap<String, Package> packages = new HashMap<String, Package>() {{
        put("ping", new PingPack(0));
    }};


    public static Package ofString(String string) {
        Package pack = null;

        if(string.contains("//START//")) {

            string = string.replace("//START//", "");

            String prefix = null;
            HashMap<String, String> VALUES = new HashMap<>();

            String[] strings = string.split("//NEXT//");
            prefix = strings[0];

            for(int i = 1; i < strings.length; i++) {
                String[] key_value = strings[i].split("=");
                VALUES.put(key_value[0], key_value[1]);
            }

            if(packages.containsKey(prefix)) {
                Package _pack = packages.get(prefix);
                pack = _pack.copy();
                pack.VALUES = VALUES;
            }


        }

        return pack;
    }

    public final String PREFIX;
    private HashMap<String, String> VALUES;

    public Package(String prefix, HashMap<String, String> values) {
        this.PREFIX = prefix;
        this.VALUES = values;
    }

    public String get(String key) {
        return this.VALUES.get(key);
    }

    private void set(String key, String value) {
        this.VALUES.remove(key);
        this.VALUES.put(key, value);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("Package{");
        builder.append("prefix=").append(this.PREFIX);

        for(String key: this.VALUES.keySet()) {
            builder.append(",").append(key).append("=").append(this.VALUES.get(key));
        }

        builder.append("}");

        return builder.toString();
    }

    String toNetworkString() {
        StringBuilder builder = new StringBuilder();

        builder.append("//START//");
        builder.append(this.PREFIX);

        for(String key:this.VALUES.keySet()) {
            builder.append("//NEXT//").append(key).append("=").append(this.VALUES.get(key));
        }

        builder.append("//END//");

        return builder.toString();
    }

    protected abstract Package copy();

    protected abstract void onServerReceive(Client client, Package pack);

    protected abstract void onClientReceive(Package pack);

}
