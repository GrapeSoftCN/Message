package interfaceApplication;

import JGrapeSystem.rMsg;
import Model.MessageModel;
import apps.appsProxy;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import nlogger.nlogger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import security.codec;
import session.session;
import string.StringHelper;

public class Message {
    private GrapeTreeDBModel message;
    private GrapeDBSpecField gDbSpecField;
    private MessageModel model;
    private session se;
    private JSONObject userInfo = null;
    private String currentWeb = null;

    public Message() {
        model = new MessageModel();

        message = new GrapeTreeDBModel();
        gDbSpecField = new GrapeDBSpecField();
        gDbSpecField.importDescription(appsProxy.tableConfig("Message"));
        message.descriptionModel(gDbSpecField);
        message.bindApp();

        se = new session();
        userInfo = se.getDatas();
        if (userInfo != null && userInfo.size() != 0) {
            currentWeb = userInfo.getString("currentWeb"); // 当前站点id
        }
    }

    /**
     * 新增留言
     * 
     * @param msgInfo
     * @return
     */
    public String AddMessage(String msgInfo) {
        int type = 1;
        String fatherid = "0";
        String result = rMsg.netMSG(100, "新增留言失败");
        if (StringHelper.InvaildString(msgInfo)) {
            return rMsg.netMSG(1, "参数不合法");
        }
        JSONObject object = JSONObject.toJSON(msgInfo);
        if (object != null && object.size() > 0) {
            if (object.containsKey("fatherid")) {
                fatherid = object.get("fatherid").toString();
                if (!fatherid.equals("0")) {
                    type = 0;
                }
            }
            result = Add(object, fatherid, type);
        }
        return result;
    }

    /**
     * 新增操作
     * 
     * @param object
     * @param fatherid
     * @param type
     *            type为0.回复留言，回复次数+1，type为1，新增留言
     * @return
     */
    @SuppressWarnings("unchecked")
    private String Add(JSONObject object, String fatherid, int type) {
        int code = 0;
        String messageContent = "", oid = "";
        String result = rMsg.netMSG(100, "新增留言失败");
        JSONObject temp = new JSONObject();
        if (object != null && object.size() > 0) {
            switch (type) {
            case 0: // 回复留言，回复次数+1
                object.put("floor", 0);
                String replynum = String.valueOf(countReply(fatherid) + 1);
                temp.put("replynum", Integer.parseInt(replynum));
                code = update(object.get("fatherid").toString(), temp.toJSONString());
            case 1: // 新增留言
                if (code == 0) {
                    object.put("wbid", currentWeb);
                    if (object.containsKey("floor")) {
                        String temps = object.getString("floor");
                        if (!temps.equals("0")) {
                            long floor = message.ne("floor", 0).count() + 1;
                            object.put("floor", new Long(floor).longValue());
                        }
                    }
                    if (object.containsKey("messageContent")) {
                        messageContent = object.get("messageContent").toString();
                    }
                    if (!StringHelper.InvaildString(messageContent)) {
                        messageContent = codec.DecodeHtmlTag(messageContent);
                        messageContent = codec.decodebase64(messageContent);
                    }
                    object.escapeHtmlPut("messageContent", messageContent);
                    oid = (String) message.data(object).insertOnce();
                }
                break;
            }
        }
        temp = FindMsgByID(oid);
        return (temp != null && temp.size() > 0) ? rMsg.netMSG(0, "新增留言成功", temp) : result;
    }

    /**
     * 修改留言
     * 
     * @param mid
     * @param msgInfo
     * @return
     */
    public String updateMessage(String mid, String msgInfo) {
        String result = rMsg.netMSG(100, "留言修改失败");
        int code = 99;
        if (!StringHelper.InvaildString(mid) && !StringHelper.InvaildString(msgInfo)) {
            code = update(mid, msgInfo);
            result = code == 0 ? rMsg.netMSG(0, "留言修改成功") : result;
        }
        return result;
    }

    // 删除留言
    public String DeleteMessage(String mid) {
        return DeleteBatchMessage(mid);
    }

    // 批量删除留言
    public String DeleteBatchMessage(String mids) {
        String[] values = null;
        long code = 0;
        String result = rMsg.netMSG(100, "删除失败");
        if (!StringHelper.InvaildString(mids)) {
            values = mids.split(",");
        }
        if (values != null) {
            message.or();
            for (String mid : values) {
                message.eq("_id", mid);
            }
            code = message.deleteAll();
            result = code > 0 ? rMsg.netMSG(0, "删除成功") : result;
        }
        return result;
    }

    /**
     * 隐藏或显示留言
     * 
     * @param mid
     * @return
     */
    @SuppressWarnings("unchecked")
    public String MaskMessage(String mids, String isdelete) {
        String[] values = null;
        long code = 0;
        JSONObject object = new JSONObject();
        object.put("isdelete", Long.parseLong(isdelete));
        String result = rMsg.netMSG(100, "留言隐藏或显示失败");
        if (!StringHelper.InvaildString(mids)) {
            values = mids.split(",");
        }
        if (values != null) {
            message.or();
            for (String mid : values) {
                message.eq("_id", mid);
            }
            code = message.data(object).updateAll();
            result = code > 0 ? rMsg.netMSG(0, "留言隐藏或显示成功") : result;
        }
        return result;
    }

    /**
     * 搜索留言
     * 
     * @param msgInfo
     * @return
     */
    public String SearchMessage(String msgInfo) {
        JSONArray array = null;
        JSONArray condArray = model.buildCond(msgInfo);
        if (condArray != null && condArray.size() > 0) {
            array = message.where(condArray).select();
        }
        return rMsg.netMSG(true, (array != null && array.size() > 0) ? model.dencode(array) : new JSONArray());
    }

    /**
     * 分页
     * 
     * @param idx
     * @param pageSize
     * @return
     */
    public String PageMessage(int idx, int pageSize) {
        return PageByMessage(idx, pageSize, null);
    }

    /**
     * 条件分页
     * 
     * @param idx
     * @param pageSize
     * @param msgInfo
     * @return
     */
    public String PageByMessage(int idx, int pageSize, String msgInfo) {
        long total = 0;
        JSONArray array = null;
        if (!StringHelper.InvaildString(msgInfo)) {
            JSONArray condArray = model.buildCond(msgInfo);
            if (condArray == null || condArray.size() <= 0) {
                return rMsg.netPAGE(idx, pageSize, total, new JSONArray());
            } else {
                message.where(condArray);
            }
        }
        if (!StringHelper.InvaildString(currentWeb)) {
            message.eq("wbid", currentWeb);
            array = message.dirty().page(idx, pageSize);
            total = message.count();
        }
        return rMsg.netPAGE(idx, pageSize, total,(array != null && array.size() > 0) ? model.dencode(array) : new JSONArray());
    }

    /**
     * 修改操作
     * 
     * @param mid
     * @param msgInfo
     * @return
     */
    private int update(String mid, String msgInfo) {
        int code = 99;
        if (!StringHelper.InvaildString(mid) && !StringHelper.InvaildString(msgInfo)) {
            JSONObject object = JSONObject.toJSON(msgInfo);
            if (object != null && object.size() > 0) {
                code = message.eq("_id", mid).data(object).update() != null ? 0 : 99;
            }
        }
        return code;
    }

    /**
     * 获取回复次数
     * 
     * @param fid
     * @return
     */
    private long countReply(String fid) {
        long code = 0;
        try {
            code = message.eq("fatherid", fid).count();
        } catch (Exception e) {
            nlogger.logout(e);
            code = 0;
        }
        return code;
    }

    /**
     * 通过唯一标识符_id,查询留言信息
     * 
     * @param mid
     * @return
     */
    private JSONObject FindMsgByID(String mid) {
        JSONObject object = null;
        if (!StringHelper.InvaildString(mid)) {
            object = message.eq("_id", mid).find();
        }
        return object != null ? model.dencode(object) : null;
    }
}
