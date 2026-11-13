package com.ferramentafacil.app.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Redimensiona e comprime as fotos antes de salvá-las
 * (economiza dados móveis e espaço no banco). Também corrige a rotação de fotos da câmera.
 * Deve ser chamada FORA da thread principal.
 */
public final class ImageUtils {

    private static final int LADO_MAXIMO = 1280;
    private static final int QUALIDADE_JPEG = 80;

    private ImageUtils() { }

    public static byte[] comprimir(Context context, Uri uri) throws IOException {
        return comprimir(context, uri, LADO_MAXIMO, QUALIDADE_JPEG);
    }

    /**
     * @param ladoMaximo maior lado da imagem final, em pixels
     * @param qualidade  qualidade JPEG (0 a 100)
     */
    public static byte[] comprimir(Context context, Uri uri, int ladoMaximo, int qualidade) throws IOException {
        ContentResolver cr = context.getContentResolver();

        // 1) Lê apenas as dimensões
        BitmapFactory.Options limites = new BitmapFactory.Options();
        limites.inJustDecodeBounds = true;
        try (InputStream in = abrir(cr, uri)) {
            BitmapFactory.decodeStream(in, null, limites);
        }
        int maior = Math.max(limites.outWidth, limites.outHeight);
        if (maior <= 0) throw new IOException("Imagem inválida");

        // 2) Decodifica já reduzida (economia de memória)
        int amostra = 1;
        while (maior / (amostra * 2) >= ladoMaximo) amostra *= 2;
        BitmapFactory.Options opcoes = new BitmapFactory.Options();
        opcoes.inSampleSize = amostra;
        Bitmap original;
        try (InputStream in = abrir(cr, uri)) {
            original = BitmapFactory.decodeStream(in, null, opcoes);
        }
        if (original == null) throw new IOException("Imagem inválida");

        // 3) Rotação EXIF
        int rotacao = 0;
        try (InputStream in = abrir(cr, uri)) {
            ExifInterface exif = new ExifInterface(in);
            int o = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (o == ExifInterface.ORIENTATION_ROTATE_90) rotacao = 90;
            else if (o == ExifInterface.ORIENTATION_ROTATE_180) rotacao = 180;
            else if (o == ExifInterface.ORIENTATION_ROTATE_270) rotacao = 270;
        } catch (Exception ignorado) {
            // Sem informação EXIF: mantém a orientação
        }

        // 4) Escala final + rotação
        float escala = Math.min(1f, ladoMaximo / (float) Math.max(original.getWidth(), original.getHeight()));
        Matrix matriz = new Matrix();
        matriz.postScale(escala, escala);
        if (rotacao != 0) matriz.postRotate(rotacao);
        Bitmap finalBmp = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matriz, true);

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        finalBmp.compress(Bitmap.CompressFormat.JPEG, qualidade, saida);
        if (finalBmp != original) original.recycle();
        finalBmp.recycle();
        return saida.toByteArray();
    }

    private static InputStream abrir(ContentResolver cr, Uri uri) throws IOException {
        InputStream in = cr.openInputStream(uri);
        if (in == null) throw new IOException("Não foi possível abrir a imagem");
        return in;
    }
}
