package pk.com.takephotoandsignature.signaturepad.utils;
/**
 * Created by pukai on 2016-4-28.
 *
 * 签名的触碰点
 */
public class Bezier {

    private TimedPoint startPoint;
    private TimedPoint control1;
    private TimedPoint control2;
    private TimedPoint endPoint;

    public TimedPoint getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(TimedPoint endPoint) {
        this.endPoint = endPoint;
    }

    public TimedPoint getControl1() {
        return control1;
    }

    public void setControl1(TimedPoint control1) {
        this.control1 = control1;
    }

    public TimedPoint getControl2() {
        return control2;
    }

    public void setControl2(TimedPoint control2) {
        this.control2 = control2;
    }

    public TimedPoint getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(TimedPoint startPoint) {
        this.startPoint = startPoint;
    }


    public Bezier set(TimedPoint startPoint, TimedPoint control1,
                  TimedPoint control2, TimedPoint endPoint) {
        this.startPoint = startPoint;
        this.control1 = control1;
        this.control2 = control2;
        this.endPoint = endPoint;
        return this;
    }

    public float length() {
        int steps = 10;
        float length = 0;
        double cx, cy, px = 0, py = 0, xDiff, yDiff;

        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            cx = point(t, this.startPoint.getX(), this.control1.getX(),
                    this.control2.getX(), this.endPoint.getX());
            cy = point(t, this.startPoint.getY(), this.control1.getY(),
                    this.control2.getY(), this.endPoint.getY());
            if (i > 0) {
                xDiff = cx - px;
                yDiff = cy - py;
                length += Math.sqrt(xDiff * xDiff + yDiff * yDiff);
            }
            px = cx;
            py = cy;
        }
        return length;

    }

    public double point(float t, float start, float c1, float c2, float end) {
        return start * (1.0 - t) * (1.0 - t) * (1.0 - t)
                + 3.0 * c1 * (1.0 - t) * (1.0 - t) * t
                + 3.0 * c2 * (1.0 - t) * t * t
                + end * t * t * t;
    }

}
