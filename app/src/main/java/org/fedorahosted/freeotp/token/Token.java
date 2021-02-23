/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fedorahosted.freeotp.token;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.net.Uri;

import com.google.android.apps.authenticator.Base32String;
import com.google.android.apps.authenticator.Base32String.DecodingException;

public class Token {
    public static class TokenUriInvalidException extends Exception {
        private static final long serialVersionUID = -1108624734612362345L;
    }

    public static enum TokenType {
        HOTP, TOTP
    }

    private static char[] STEAMCHARS = new char[] {
            '2', '3', '4', '5', '6', '7', '8', '9', 'B', 'C',
            'D', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P', 'Q',
            'R', 'T', 'V', 'W', 'X', 'Y'};

    private String issuerInt;
    private String issuerExt;
    private String issuerAlt;
    private String label;
    private String labelAlt;
    private String image;
    private String imageAlt;
    private TokenType type;
    private String algo;
    private byte[] secret;
    private int digits;
    private long counter;
    private int period;

    private Token(Uri uri, boolean internal) throws TokenUriInvalidException {
        if (!Objects.equals(uri.getScheme(), "otpauth"))
            throw new TokenUriInvalidException();

        if (Objects.equals(uri.getAuthority(), "totp"))
            type = TokenType.TOTP;
        else if (Objects.equals(uri.getAuthority(), "hotp"))
            type = TokenType.HOTP;
        else
            throw new TokenUriInvalidException();

        String path = uri.getPath();
        if (path == null)
            throw new TokenUriInvalidException();

        // Strip the path of its leading '/'
        for (int i = 0; path.charAt(i) == '/'; i++)
            path = path.substring(1);
        if (path.length() == 0)
            throw new TokenUriInvalidException();

        int i = path.indexOf(':');
        issuerExt = i < 0 ? "" : path.substring(0, i);
        issuerInt = uri.getQueryParameter("issuer");
        label = path.substring(i >= 0 ? i + 1 : 0);

        algo = uri.getQueryParameter("algorithm");
        if (algo == null)
            algo = "sha1";
        algo = algo.toUpperCase(Locale.US);
        try {
            Mac.getInstance("Hmac" + algo);
        } catch (NoSuchAlgorithmException e1) {
            throw new TokenUriInvalidException();
        }

        try {
            String d = uri.getQueryParameter("digits");
            if (d == null) {
                if (issuerExt.equals("Steam"))
                    d = "5";
                else
                    d = "6";
            }
            digits = Integer.parseInt(d);
            if (!issuerExt.equals("Steam") && digits != 6 && digits != 7 && digits != 8 && digits != 5)
                throw new TokenUriInvalidException();
        } catch (NumberFormatException e) {
            throw new TokenUriInvalidException();
        }

        try {
            String p = uri.getQueryParameter("period");
            if (p == null)
                p = "30";
            period = Integer.parseInt(p);
        } catch (NumberFormatException e) {
            throw new TokenUriInvalidException();
        }

        if (type == TokenType.HOTP) {
            try {
                String c = uri.getQueryParameter("counter");
                if (c == null)
                    c = "0";
                counter = Long.parseLong(c) - 1;
            } catch (NumberFormatException e) {
                throw new TokenUriInvalidException();
            }
        }

        try {
            String s = uri.getQueryParameter("secret");
            secret = Base32String.decode(s);
        } catch (DecodingException e) {
            throw new TokenUriInvalidException();
        } catch (NullPointerException e) {
            throw new TokenUriInvalidException();
        }

        image = uri.getQueryParameter("image");

        if (internal) {
            setIssuer(uri.getQueryParameter("issueralt"));
            setLabel(uri.getQueryParameter("labelalt"));
        }
    }

    private String getHOTP(long counter) {
        // Encode counter in network byte order
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(counter);

        // Create digits divisor
        int div = 1;
        for (int i = digits; i > 0; i--)
            div *= 10;

        // Create the HMAC
        try {
            Mac mac = Mac.getInstance("Hmac" + algo);
            mac.init(new SecretKeySpec(secret, "Hmac" + algo));

            // Do the hashing
            byte[] digest = mac.doFinal(bb.array());

            // Truncate
            int binary;
            int off = digest[digest.length - 1] & 0xf;
            binary = (digest[off] & 0x7f) << 0x18;
            binary |= (digest[off + 1] & 0xff) << 0x10;
            binary |= (digest[off + 2] & 0xff) << 0x08;
            binary |= (digest[off + 3] & 0xff);

            String hotp = "";
            if (issuerExt.equals("Steam")) {
                for (int i = 0; i < digits; i++) {
                    hotp += STEAMCHARS[binary % STEAMCHARS.length];
                    binary /= STEAMCHARS.length;
                }
            } else {
                binary = binary % div;

                // Zero pad
                hotp = Integer.toString(binary);
                while (hotp.length() != digits)
                    hotp = "0" + hotp;
            }

            return hotp;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return "";
    }

    public Token(String uri, boolean internal) throws TokenUriInvalidException {
        this(Uri.parse(uri), internal);
    }

    public Token(Uri uri) throws TokenUriInvalidException {
        this(uri, false);
    }

    public Token(String uri) throws TokenUriInvalidException {
        this(Uri.parse(uri));
    }

    public String getID() {
        String id;
        if (issuerInt != null && !issuerInt.equals(""))
            id = issuerInt + ":" + label;
        else if (issuerExt != null && !issuerExt.equals(""))
            id = issuerExt + ":" + label;
        else
            id = label;

        return id;
    }

    // NOTE: This changes internal data. You MUST save the token immediately.
    public void setIssuer(String issuer) {
        issuerAlt = (issuer == null || issuer.equals(this.issuerExt)) ? null : issuer;
    }

    public String getIssuer() {
        if (issuerAlt != null)
            return issuerAlt;
        return issuerExt != null ? issuerExt : "";
    }

    // NOTE: This changes internal data. You MUST save the token immediately.
    public void setLabel(String label) {
        labelAlt = (label == null || label.equals(this.label)) ? null : label;
    }

    public String getLabel() {
        if (labelAlt != null)
            return labelAlt;
        return label != null ? label : "";
    }

    public int getDigits() {
        return digits;
    }

    // NOTE: This may change internal data. You MUST save the token immediately.
    public TokenCode generateCodes() {
        long cur = System.currentTimeMillis();

        switch (type) {
        case HOTP:
            return new TokenCode(getHOTP(counter++), cur, cur + (period * 1000));

        case TOTP:
            long counter = cur / 1000 / period;
            return new TokenCode(getHOTP(counter + 0),
                                 (counter + 0) * period * 1000,
                                 (counter + 1) * period * 1000,
                   new TokenCode(getHOTP(counter + 1),
                                 (counter + 1) * period * 1000,
                                 (counter + 2) * period * 1000));
        }

        return null;
    }

    public TokenType getType() {
        return type;
    }

    public Uri toUri() {
        String issuerLabel = !issuerExt.equals("") ? issuerExt + ":" + label : label;

        Uri.Builder builder = new Uri.Builder().scheme("otpauth").path(issuerLabel)
                .appendQueryParameter("secret", Base32String.encode(secret))
                .appendQueryParameter("issuer", issuerInt == null ? issuerExt : issuerInt)
                .appendQueryParameter("algorithm", algo)
                .appendQueryParameter("digits", Integer.toString(digits))
                .appendQueryParameter("period", Integer.toString(period));

        switch (type) {
        case HOTP:
            builder.authority("hotp");
            builder.appendQueryParameter("counter", Long.toString(counter + 1));
            break;
        case TOTP:
            builder.authority("totp");
            break;
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return toUri().toString();
    }

    public void setImage(Uri image) {
        imageAlt = null;
        if (image == null)
            return;

        if (this.image == null || !Uri.parse(this.image).equals(image))
            imageAlt = image.toString();
    }

    public Uri getImage() {
        if (imageAlt != null)
            return Uri.parse(imageAlt);

        if (image != null)
            return Uri.parse(image);

        return null;
    }

    public String getSecret() {
        return Base32String.encode(secret);
    }

    public String getAlgorithm() {
        return algo;
    }

    public int getPeriod() {
        return period;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return digits == token.digits &&
                counter == token.counter &&
                period == token.period &&
                Objects.equals(issuerInt, token.issuerInt) &&
                Objects.equals(issuerExt, token.issuerExt) &&
                Objects.equals(issuerAlt, token.issuerAlt) &&
                Objects.equals(label, token.label) &&
                Objects.equals(labelAlt, token.labelAlt) &&
                Objects.equals(image, token.image) &&
                Objects.equals(imageAlt, token.imageAlt) &&
                type == token.type &&
                Objects.equals(algo, token.algo) &&
                Arrays.equals(secret, token.secret);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(issuerInt, issuerExt, issuerAlt, label, labelAlt, image, imageAlt, type, algo, digits, counter, period);
        result = 31 * result + Arrays.hashCode(secret);
        return result;
    }
}
