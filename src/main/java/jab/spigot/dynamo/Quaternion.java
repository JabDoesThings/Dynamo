package jab.spigot.dynamo;

import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class Quaternion {

    public double x;
    public double y;
    public double z;
    public double w;

    private static final Quaternion quatTmp1 = new Quaternion();
    private static final Quaternion quatTmp2 = new Quaternion();
    private static final Quaternion quatTmp3 = new Quaternion();
    private static final Quaternion quatTmp4 = new Quaternion();

    public Quaternion() {
    }

    public Quaternion(EulerAngle angle) {
        double heading = angle.getX();
        double bank = angle.getY();
        double attitude = angle.getZ();
        double c1 = Math.cos(heading / 2);
        double s1 = Math.sin(heading / 2);
        double c2 = Math.cos(attitude / 2);
        double s2 = Math.sin(attitude / 2);
        double c3 = Math.cos(bank / 2);
        double s3 = Math.sin(bank / 2);
        double c1c2 = c1 * c2;
        double s1s2 = s1 * s2;
        w = c1c2 * c3 - s1s2 * s3;
        x = c1c2 * s3 + s1s2 * c3;
        y = s1 * c2 * c3 + c1 * s2 * s3;
        z = c1 * s2 * c3 - s1 * c2 * s3;
    }

    public Quaternion(double angle, Vector rotationAxis) {
        x = rotationAxis.getX() * Math.sin(angle / 2);
        y = rotationAxis.getY() * Math.sin(angle / 2);
        z = rotationAxis.getZ() * Math.sin(angle / 2);
        w = Math.cos(angle / 2);
    }

    public Quaternion(double w, double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public void set(double angle, Vector rotationAxis) {
        x = rotationAxis.getX() * Math.sin(angle / 2);
        y = rotationAxis.getY() * Math.sin(angle / 2);
        z = rotationAxis.getZ() * Math.sin(angle / 2);
        w = Math.cos(angle / 2);
    }

    public void set(double w, double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public void set(Quaternion q) {
        this.x = q.x;
        this.y = q.y;
        this.z = q.z;
        this.w = q.w;
    }

    public double getSize() {
        return Math.sqrt(w * w + x * x + y * y + z * z);
    }

    public void normalize() {
        double sizeInv = 1 / getSize();
        x *= sizeInv;
        y *= sizeInv;
        z *= sizeInv;
        w *= sizeInv;
    }

    public void multiply(Quaternion qb, Quaternion result) {
        Quaternion qa = this;
        quatTmp4.w = (qa.w * qb.w) - (qa.x * qb.x) - (qa.y * qb.y) - (qa.z * qb.z);
        quatTmp4.x = (qa.x * qb.w) + (qa.w * qb.x) + (qa.y * qb.z) - (qa.z * qb.y);
        quatTmp4.y = (qa.y * qb.w) + (qa.w * qb.y) + (qa.z * qb.x) - (qa.x * qb.z);
        quatTmp4.z = (qa.z * qb.w) + (qa.w * qb.z) + (qa.x * qb.y) - (qa.y * qb.x);
        result.set(quatTmp4);
    }

    public void conjugate() {
        // w = w;
        x = -x;
        y = -y;
        z = -z;
    }

    // P' = Q.P.Q*
    public void rotate(Vector point, Vector rotatedPoint) {
        quatTmp1.set(0, point.getX(), point.getY(), point.getZ());
        multiply(quatTmp1, quatTmp2);
        quatTmp1.set(this);
        quatTmp1.conjugate();
        quatTmp2.multiply(quatTmp1, quatTmp3);

        rotatedPoint.setX(quatTmp3.x);
        rotatedPoint.setY(quatTmp3.y);
        rotatedPoint.setZ(quatTmp3.z);
    }

    public void rotate(EulerAngle angle) {
        rotate(angle.getX(), angle.getY(), angle.getZ());
    }

    public void rotate(double heading, double bank, double attitude) {
        double c1 = Math.cos(heading);
        double s1 = Math.sin(heading);
        double c2 = Math.cos(attitude);
        double s2 = Math.sin(attitude);
        double c3 = Math.cos(bank);
        double s3 = Math.sin(bank);
        w = Math.sqrt(1.0 + c1 * c2 + c1 * c3 - s1 * s2 * s3 + c2 * c3) / 2.0;
        double w4 = (4.0 * w);
        x = (c2 * s3 + c1 * s3 + s1 * s2 * c3) / w4;
        y = (s1 * c2 + s1 * c3 + c1 * s2 * s3) / w4;
        z = (-s1 * s3 + c1 * s2 * c3 + s2) / w4;
    }

    public EulerAngle toEulerAngle() {
        double sqw = w * w;
        double sqx = x * x;
        double sqy = y * y;
        double sqz = z * z;
        double heading = Math.atan2(2.0 * (x * y + z * w), (sqx - sqy - sqz + sqw));
        double bank = Math.atan2(2.0 * (y * z + x * w), (-sqx - sqy + sqz + sqw));
        double attitude = Math.asin(-2.0 * (x * z - y * w) / (sqx + sqy + sqz + sqw));
        return new EulerAngle(heading, bank, attitude);
    }
}