package com.nexgo.emvsample;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.nexgo.oaf.apiv3.emv.AidEntity;
import com.nexgo.oaf.apiv3.emv.CapkEntity;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * cn.nexgo.inbas.components.emv
 * Author: zhaojiangon 2018/1/31 17:31.
 * Modified by Brad2018/4/23 : add test file
 */

public class EmvUtils {
    static Context context;
    public EmvUtils(Context context){
        this.context = context;
    }

    public static List<AidEntity> getAidList(){
        List<AidEntity> aidEntityList = new ArrayList<>();
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonArray jsonArray;

        jsonArray = parser.parse(readAssetsTxt("inbas_aid.json")).getAsJsonArray();

        if (jsonArray == null){
            return null;
        }

        for (JsonElement user : jsonArray) {
            AidEntity userBean = gson.fromJson(user, AidEntity.class);
            aidEntityList.add(userBean);
        }
        return aidEntityList;
    }


    public static List<CapkEntity> getCapkList(){
        List<CapkEntity> capkEntityList = new ArrayList<>();
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();

        JsonArray jsonArray;

        jsonArray = parser.parse(readAssetsTxt("inbas_capk.json")).getAsJsonArray();

        if (jsonArray == null){
            return null;
        }

        for (JsonElement user : jsonArray) {
            CapkEntity userBean = gson.fromJson(user, CapkEntity.class);
            capkEntityList.add(userBean);
        }
        return capkEntityList;
    }




    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static String readAssetsTxt(String fileName) {
        try {
            InputStream is = context.getAssets().open(fileName);

            int size = is.available();
            // Read the entire asset into a local byte buffer.
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            is.close();
            // Convert the buffer into a string.
            return new String(buffer, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static String readFile(String filePath) {
        try {
            InputStream is = new FileInputStream(filePath);
            int size = is.available();
            // Read the entire asset into a local byte buffer.
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            is.close();
            // Convert the buffer into a string.
            return new String(buffer, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static String getElementAttr(Element element, String attr) {
        if (element == null) return "";
        if (element.hasAttribute(attr)) {
            return element.getAttribute(attr);
        }
        return "";
    }

    private static Element[] getElementsByName(Element parent, String name) {
        ArrayList resList = new ArrayList();
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node nd = nl.item(i);
            if (nd.getNodeName().equals(name)) {
                resList.add(nd);
            }
        }
        Element[] res = new Element[resList.size()];
        for (int i = 0; i < resList.size(); i++) {
            res[i] = (Element) resList.get(i);
        }
        return res;
    }

    public static String HexToASCII(String hexString){
        if (hexString == null || hexString.compareTo("") == 0)
            return "";

        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hexString.length(); i+=2) {
            String str = hexString.substring(i, i+2);
            output.append((char)Integer.parseInt(str, 16));
        }
        return new String("" + output);
    }

}
