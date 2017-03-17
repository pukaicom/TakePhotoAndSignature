package pk.com.takephotoandsignature.signaturepad.utils;

/**
 * Created by pukai on 2016-4-27.
 * 当前触碰的点
 */
public class TimedPoint {

    private float x;
    private float y;
    private long timestamp;

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public TimedPoint set(float x, float y) {
        this.x = x;
        this.y = y;
        this.timestamp = System.currentTimeMillis();
        return this;
    }

    public float velocityFrom(TimedPoint start) {
        float velocity = distanceTo(start) / (this.timestamp - start.timestamp);
        return velocity;
    }

    public float distanceTo(TimedPoint point) {
        return (float) Math.sqrt(Math.pow(point.x - this.x, 2) + Math.pow(point.y - this.y, 2));
    }
}
