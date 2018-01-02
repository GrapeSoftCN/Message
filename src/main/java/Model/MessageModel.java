package Model;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import common.java.database.dbFilter;
import common.java.string.StringHelper;


public class MessageModel {
    /**
     * 整合参数，将JSONObject类型的参数封装成JSONArray类型
     * 
     * @param object
     * @return
     */
    public JSONArray buildCond(String Info) {
        String key;
        Object value;
        JSONArray condArray = null;
        JSONObject object = JSONObject.toJSON(Info);
        dbFilter filter = new dbFilter();
        if (object != null && object.size() > 0) {
            for (Object object2 : object.keySet()) {
                key = object2.toString();
                value = object.get(key);
                filter.eq(key, value);
            }
            condArray = filter.build();
        } else {
            condArray = JSONArray.toJSONArray(Info);
        }
        return condArray;
    }

    /**
     * 留言内容解码
     * 
     * @param array
     * @return
     */
    @SuppressWarnings("unchecked")
    public JSONArray dencode(JSONArray array) {
        if (array.size() == 0) {
            return array;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject object = (JSONObject) array.get(i);
            array.set(i, dencode(object));
        }
        return array;
    }

    /**
     * 留言内容解码
     * 
     * @param array
     * @return
     */
    @SuppressWarnings("unchecked")
    public JSONObject dencode(JSONObject object) {
        String content = "";
        if (object == null || object.size() <= 0) {
            return new JSONObject();
        }
        if (object.containsKey("content")) {
            content = object.getString("content");
            if (StringHelper.InvaildString(content)) {
                object.put("content", object.escapeHtmlGet("content"));
            }
        }
        return object;
    }
}
