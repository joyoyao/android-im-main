package io.rong.imkit.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import io.rong.imkit.R;
import io.rong.imkit.RongContext;

public class RongDateUtils {

    private static final int OTHER = 2014;
    private static final int TODAY = 6;
    private static final int YESTERDAY = 15;

    public static int judgeDate(Date date) {
        // 今天
        Calendar calendarToday = Calendar.getInstance();
        calendarToday.set(Calendar.HOUR_OF_DAY, 0);
        calendarToday.set(Calendar.MINUTE, 0);
        calendarToday.set(Calendar.SECOND, 0);
        calendarToday.set(Calendar.MILLISECOND, 0);
        // 昨天
        Calendar calendarYesterday = Calendar.getInstance();
        calendarYesterday.add(Calendar.DATE, -1);
        calendarYesterday.set(Calendar.HOUR, 0);
        calendarYesterday.set(Calendar.MINUTE, 0);
        calendarYesterday.set(Calendar.SECOND, 0);
        calendarYesterday.set(Calendar.MILLISECOND, 0);

        // 目标日期
        Calendar calendarTarget = Calendar.getInstance();
        calendarTarget.setTime(date);

        if (calendarTarget.before(calendarYesterday)) {// 是否在calendarT之前
            return OTHER;
        } else if (calendarTarget.before(calendarToday)) {
            return YESTERDAY;
        } else {
            return TODAY;
        }

    }

    public static String getConversationListFormatDate(Date date) {

        if (date == null)
            return "";

        String formatDate = null;

        int type = judgeDate(date);

        switch (type) {
            case TODAY:
                formatDate = formatDate(date, "HH:mm");
                break;

            case YESTERDAY:
                String formatString = RongContext.getInstance().getResources().getString(R.string.rc_yesterday_format);
                formatDate = String.format(formatString, formatDate(date, "HH:mm"));
                break;
            case OTHER:
                formatDate = formatDate(date, "yyyy-MM-dd");
                break;

            default:
                break;
        }

        return formatDate;

    }

    public static String getConversationFormatDate(Date date) {

        if (date == null)
            return "";

        String formatedDate = null;

        int type = judgeDate(date);

        switch (type) {
            case TODAY:
                formatedDate = formatDate(date, "HH:mm");
                break;

            case YESTERDAY:
                String formatString = RongContext.getInstance().getResources().getString(R.string.rc_yesterday_format);
                formatedDate = String.format(formatString, formatDate(date, "HH:mm"));
                break;

            case OTHER:
                formatedDate = formatDate(date, "yyyy-MM-dd HH:mm");
                break;

            default:
                break;
        }

        return formatedDate;

    }

    /**
     *
     * @param currentTime
     * @param preTime
     * @return  true 大于1钟  false 小于等于
     */
    public static boolean isShowChatTime(long currentTime, long preTime) {

        int typeCurrent = judgeDate(new Date(currentTime));
        int typePre = judgeDate(new Date(preTime));

        if (typeCurrent == typePre) {

            if ((currentTime - preTime) > 60 * 1000) {
                return true;
            } else {
                return false;
            }

        } else {
            return true;
        }

        // return typeCurrent == typePre ? (((currentTime - preTime) > 60 *
        // 1000) ? true : false) : true;

    }

    public static String formatDate(Date date, String fromat) {
        SimpleDateFormat sdf = new SimpleDateFormat(fromat);
        return sdf.format(date);
    }

}
