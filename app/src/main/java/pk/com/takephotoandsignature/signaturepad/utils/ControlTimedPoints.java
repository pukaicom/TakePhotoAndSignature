package pk.com.takephotoandsignature.signaturepad.utils;

/**
 * Created by pukai on 2016-4-27.
 *
 * 触碰点控制
 */
public class ControlTimedPoints {

    private TimedPoint c1;
    private TimedPoint c2;

    public TimedPoint getC1() {
        return c1;
    }

    public void setC1(TimedPoint c1) {
        this.c1 = c1;
    }

    public TimedPoint getC2() {
        return c2;
    }

    public void setC2(TimedPoint c2) {
        this.c2 = c2;
    }

    public ControlTimedPoints set(TimedPoint c1, TimedPoint c2) {
        this.c1 = c1;
        this.c2 = c2;
        return this;
    }

}
