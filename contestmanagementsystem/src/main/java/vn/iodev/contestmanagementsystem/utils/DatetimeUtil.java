package vn.iodev.contestmanagementsystem.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.springframework.util.ObjectUtils;


public class DatetimeUtil {
	
	public static final String _VN_DATE_FORMAT = "dd/MM/yyyy";
	
	
	public static String dateToString(Date date, String pattern) {

        SimpleDateFormat formatter = new SimpleDateFormat(pattern);

        if (date == null || ObjectUtils.isEmpty(pattern)) {
            return "";
        }

        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);

        return formatter.format(calendar.getTime());
    }
}
