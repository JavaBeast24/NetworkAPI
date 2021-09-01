package com.beastdevelopment.javabeast.networkapi.defaultpacks;

import com.beastdevelopment.javabeast.networkapi.Client;
import com.beastdevelopment.javabeast.networkapi.Package;

import java.util.Date;
import java.util.HashMap;

public class PingPack extends Package {
    public PingPack(long time) {
        super("ping", new HashMap<String, String>() {{put("time", time+"");}});
    }

    @Override
    protected Package copy() {
        return new PingPack(Long.parseLong(get("time")));
    }

    @Override
    protected void onServerReceive(Client client, Package pack) {
        try {
            long ping = new Date().getTime() - Long.parseLong(pack.get("time"));
            client.send(new PingPack(ping));
        }catch (Exception ignored) { }
    }

    @Override
    protected void onClientReceive(Package pack) {
        System.out.println("ping "+pack.get("time")+" ms");
    }
}
