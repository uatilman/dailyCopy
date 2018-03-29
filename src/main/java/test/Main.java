package test;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Main {

    public static  void main(String[] args) {


        Calendar shutdownTime = Calendar.getInstance();
        shutdownTime.set(Calendar.HOUR_OF_DAY, 18);
        shutdownTime.set(Calendar.MINUTE, 0);

        long waitTime = (shutdownTime.getTimeInMillis()- Calendar.getInstance().getTimeInMillis()) /1000 ;
        System.out.println(waitTime);


    }
}
