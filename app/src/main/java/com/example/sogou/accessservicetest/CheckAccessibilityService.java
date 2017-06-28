package com.example.sogou.accessservicetest;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import java.util.List;


public class CheckAccessibilityService extends AccessibilityService {

    static final String WECHAT_PACKAGENAME = "com.tencent.mm";  //微信包名
    static final String NOTIFICATION_TEXT = "微信：你收到了一条消息";

    static final String LISTVIEW_CONTAINER_ID = "com.tencent.mm:id/a3e";
    static final String SIMPLE_TEXT_ID = "com.tencent.mm:id/im";
    static final String USER_IMAGE_ID = "com.tencent.mm:id/ik";

    private String chatName;
    private String chatRecord;


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                List<AccessibilityNodeInfo> lists = rootNode.findAccessibilityNodeInfosByViewId(LISTVIEW_CONTAINER_ID);
                if(lists.size() > 0) {
                    AccessibilityNodeInfo listViewNode = lists.get(0);
                    getLatestRecord(listViewNode);
                }
                break;
        }
    }

    private void getLatestRecord(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo latestNode = node.getChild(node.getChildCount() - 1);  //获取最新添加的container
        getChatName(latestNode);
        List<AccessibilityNodeInfo> lists = latestNode.findAccessibilityNodeInfosByViewId(SIMPLE_TEXT_ID);
        if(lists.size() > 0) {
            chatRecord = lists.get(0).getText().toString();
            Toast.makeText(this, chatName + "---" + chatRecord, Toast.LENGTH_SHORT).show();
        }
    }


    private void getChatName(AccessibilityNodeInfo node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo node1 = node.getChild(i);
            if ("android.widget.ImageView".equals(node1.getClassName()) && node1.isClickable()) {
                if (!TextUtils.isEmpty(node1.getContentDescription())) {
                    chatName = node1.getContentDescription().toString();
                    if (chatName.contains("头像")) {
                        chatName = chatName.replace("头像", "");
                        return ;
                    }
                }
            }
            getChatName(node1);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("onDestroy", "onDestroy");
    }

    @Override
    public void onInterrupt() {
        Toast.makeText(this, "onInterrupt", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Toast.makeText(this, "onServiceConnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Toast.makeText(this, "onUnbind", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }
}
