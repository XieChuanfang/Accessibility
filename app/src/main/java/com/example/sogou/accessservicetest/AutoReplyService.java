package com.example.sogou.accessservicetest;


import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

/**
 * Created by Administrator on 2017/6/20.
 */

public class AutoReplyService extends AccessibilityService {

    private final static String MM_PNAME = "com.tencent.mm";  //微信包名
    private Handler handler = new Handler();
    static final String ANSWER_TEXT = "正在忙,稍后回复你";

    static final String LISTVIEW_CONTAINER_ID = "com.tencent.mm:id/a3e";
    static final String SIMPLE_TEXT_ID = "com.tencent.mm:id/im";
    static final String USER_IMAGE_ID = "com.tencent.mm:id/ik";

    private String curName;
    private String curContent;

    private boolean sent = false;   //是否允许处理scrool事件


    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:  //通知栏事件
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        if (!TextUtils.isEmpty(content)) {
                            openNotification(event);
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (fill(ANSWER_TEXT)) {
                                        send();
                                    }
                                }
                            }, 1000);
                        }
                    }
                }

                break;

            case AccessibilityEvent.TYPE_VIEW_SCROLLED:

                if(sent) {
                    sent = false;
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    List<AccessibilityNodeInfo> lists = rootNode.findAccessibilityNodeInfosByViewId(LISTVIEW_CONTAINER_ID);
                    if(lists.size() > 0) {
                        AccessibilityNodeInfo listViewNode = lists.get(0);
                        AccessibilityNodeInfo latestNode = listViewNode.getChild(listViewNode.getChildCount() - 1);  //获取最新添加的container
                        if(getChatName(latestNode) && receiveMessage(latestNode)) {
                            Toast.makeText(this, curName + "---" + curContent , Toast.LENGTH_SHORT).show();
                            if(fill(ANSWER_TEXT)) {
                                send();
                            }
                        }
                    }
                }

                break;
        }
    }

    /**
     * 打开通知
     * @param event
     */
    private void openNotification(AccessibilityEvent event) {
        if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
            Notification notification = (Notification) event.getParcelableData();
            PendingIntent pendingIntent = notification.contentIntent;
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean fill(String content) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            return findEditText(rootNode, content);
        }
        return false;
    }

    private boolean findEditText(AccessibilityNodeInfo rootNode, String content) {
        int count = rootNode.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo nodeInfo = rootNode.getChild(i);
            if (nodeInfo == null) {
                continue;
            }

            if ("android.widget.EditText".equals(nodeInfo.getClassName())) {
                Bundle arguments = new Bundle();
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
                arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                        true);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
                        arguments);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                ClipData clip = ClipData.newPlainText("label", content);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clip);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                return true;
            }

            if (findEditText(nodeInfo, content)) {
                return true;
            }
        }

        return false;
    }

    private void send() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("发送");
            if (list != null && list.size() > 0) {
                for (AccessibilityNodeInfo node : list) {
                    if (node.getClassName().equals("android.widget.Button") && node.isEnabled()) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                        //延迟500ms设置
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                sent = true;
                            }
                        }, 500);
                    }
                }
            }
        }
    }

    private boolean receiveMessage(AccessibilityNodeInfo node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo node1 = node.getChild(i);
            if ("android.widget.TextView".equals(node1.getClassName()) && node1.isLongClickable()) {
                curContent = node1.getText().toString();
                return true;
            }

            if(receiveMessage(node1)) {
                return true;
            }
        }
        return false;
    }


    private boolean getChatName(AccessibilityNodeInfo node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo node1 = node.getChild(i);
            if ("android.widget.ImageView".equals(node1.getClassName()) && node1.isClickable()) {
                if (!TextUtils.isEmpty(node1.getContentDescription())) {
                    curName = node1.getContentDescription().toString();
                    if (curName.contains("头像")) {
                        curName = curName.replace("头像", "");
                        return true;
                    }
                }
            }
            if(getChatName(node1)) {
                return true;
            }
        }
        return false;
    }



    @Override
    public void onInterrupt() {

    }
}
