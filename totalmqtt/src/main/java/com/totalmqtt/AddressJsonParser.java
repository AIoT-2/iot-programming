package com.totalmqtt;

import java.io.*;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class AddressJsonParser {
    private String jsonLocation;
    private Map<String, Object> datas;

    public AddressJsonParser() {
        jsonLocation = "";
        datas = new HashMap<>();
    }

    public void setJsonLocation(String loca) {
        jsonLocation = loca;
    }

    public void parsing() {
        try {
            JSONParser parser = new JSONParser();
            Reader reader = new FileReader(jsonLocation);
            JSONArray jsonArray = (JSONArray) parser.parse(reader);
            for (Object data : jsonArray) {
                JSONObject jsonObject = (JSONObject) data;
                datas.put((String) jsonObject.get("use"), (JSONObject) jsonObject.get("address"));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public Map<String, Object> getAddress(String use) {
        Map<String, Object> result = new HashMap<>();
        if (datas.containsKey(use)) {
            Object obejct = datas.get(use);
            JSONObject jsonObject = (JSONObject) obejct;
            result.put("ip", jsonObject.get("ip"));
            result.put("port", jsonObject.get("port"));
            return result;
        }
        return null;
    }
}
