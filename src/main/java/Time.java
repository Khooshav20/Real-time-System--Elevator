import java.io.Serializable;

public class Time implements Serializable {
    private final int h;
    private final int min;
    private final float s;
    public Time(int hours, int minutes, float second){
        h = hours;
        min  = minutes;
        s = second;
    }

    public Time (String hours, String minutes, String second){
        h = Integer.parseInt(hours);
        min = Integer.parseInt(minutes);
        s = Float.parseFloat(second);
    }

    public String toString(){
        return h + ":" + min + ":" + s;
    }

}
