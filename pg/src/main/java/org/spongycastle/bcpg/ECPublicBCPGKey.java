package org.spongycastle.bcpg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.x9.ECNamedCurveTable;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.ec.CustomNamedCurves;
import org.spongycastle.math.ec.ECAlgorithms;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.BigIntegers;

/**
 * base class for an EC Public Key.
 */
public abstract class ECPublicBCPGKey
    extends BCPGObject
    implements BCPGKey
{
    ASN1ObjectIdentifier oid;
    ECPoint point;

    /**
     * @param in the stream to read the packet from.
     */
    protected ECPublicBCPGKey(
        BCPGInputStream in)
        throws IOException
    {
            StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
            StringBuilder sb = new StringBuilder();
            sb.append(ste.getMethodName())        // メソッド名取得
                .append("(")
                .append(ste.getFileName())        // ファイル名取得
                .append(":")
                .append(ste.getLineNumber())    // 行番号取得
                .append(")");
            System.out.println(sb.toString());
        this.oid = ASN1ObjectIdentifier.getInstance(ASN1Primitive.fromByteArray(readBytesOfEncodedLength(in)));
        this.point = decodePoint(new MPInteger(in).getValue(), oid);
    }

    protected ECPublicBCPGKey(
        ASN1ObjectIdentifier oid,
        ECPoint point)
    {
        this.point = point.normalize();
        this.oid = oid;
    }

    protected ECPublicBCPGKey(
        ASN1ObjectIdentifier oid,
        BigInteger encodedPoint)
        throws IOException
    {
        this.point = decodePoint(encodedPoint, oid);
        this.oid = oid;
    }

    /**
     * return "PGP"
     *
     * @see org.spongycastle.bcpg.BCPGKey#getFormat()
     */
    public String getFormat()
    {
        return "PGP";
    }

    /**
     * return the standard PGP encoding of the key.
     *
     * @see org.spongycastle.bcpg.BCPGKey#getEncoded()
     */
    public byte[] getEncoded()
    {
        try
        {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            BCPGOutputStream pgpOut = new BCPGOutputStream(bOut);

            pgpOut.writeObject(this);

            return bOut.toByteArray();
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public void encode(
        BCPGOutputStream out)
        throws IOException
    {
        byte[] oid = this.oid.getEncoded();
        out.write(oid, 1, oid.length - 1);

        MPInteger point = new MPInteger(new BigInteger(1, this.point.getEncoded()));
        out.writeObject(point);
    }

    /**
     * @return point
     */
    public ECPoint getPoint()
    {
        return point;
    }

    /**
     * @return oid
     */
    public ASN1ObjectIdentifier getCurveOID()
    {
        return oid;
    }

    protected static byte[] readBytesOfEncodedLength(
        BCPGInputStream in)
        throws IOException
    {
        int length = in.read();
        if (length == 0 || length == 0xFF)
        {
            throw new IOException("future extensions not yet implemented.");
        }

        byte[] buffer = new byte[length + 2];
        in.readFully(buffer, 2, buffer.length - 2);
        buffer[0] = (byte)0x06;
        buffer[1] = (byte)length;

        return buffer;
    }

    private static ECPoint decodePoint(
        BigInteger encodedPoint,
        ASN1ObjectIdentifier oid)
        throws IOException
    {
            StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
            StringBuilder sb = new StringBuilder();
            sb.append(ste.getMethodName())        // メソッド名取得
                .append("(")
                .append(ste.getFileName())        // ファイル名取得
                .append(":")
                .append(ste.getLineNumber())    // 行番号取得
                .append(")");
            System.out.println(sb.toString());
        X9ECParameters x9 = CustomNamedCurves.getByOID(oid);
        if (x9 == null)
        {
            x9 = ECNamedCurveTable.getByOID(oid);
            if (x9 == null)
            {
                throw new IOException(oid.getId() + " does not match any known curve.");
            }
        }
        if (!ECAlgorithms.isFpCurve(x9.getCurve()))
        {
            throw new IOException("Only prime field curves are supported.");
        }
        return x9.getCurve().decodePoint(BigIntegers.asUnsignedByteArray(encodedPoint));
    }
}
