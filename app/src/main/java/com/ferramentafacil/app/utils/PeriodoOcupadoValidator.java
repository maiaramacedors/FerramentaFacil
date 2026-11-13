package com.ferramentafacil.app.utils;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.ferramentafacil.app.model.Periodo;
import com.google.android.material.datepicker.CalendarConstraints;

import java.util.List;

/**
 * Bloqueia no calendário (MaterialDatePicker) os dias em que a ferramenta já está reservada.
 * Precisa ser Parcelable porque o calendário é um DialogFragment que pode ser recriado.
 */
public class PeriodoOcupadoValidator implements CalendarConstraints.DateValidator {

    private final long[] inicios;
    private final long[] fins;

    public PeriodoOcupadoValidator(List<Periodo> periodos) {
        inicios = new long[periodos.size()];
        fins = new long[periodos.size()];
        for (int i = 0; i < periodos.size(); i++) {
            inicios[i] = periodos.get(i).inicio;
            fins[i] = periodos.get(i).fim;
        }
    }

    protected PeriodoOcupadoValidator(Parcel in) {
        inicios = in.createLongArray();
        fins = in.createLongArray();
    }

    @Override
    public boolean isValid(long data) {
        for (int i = 0; i < inicios.length; i++) {
            if (DateUtils.diaOcupado(data, inicios[i], fins[i])) return false;
        }
        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLongArray(inicios);
        dest.writeLongArray(fins);
    }

    public static final Creator<PeriodoOcupadoValidator> CREATOR = new Creator<PeriodoOcupadoValidator>() {
        @Override
        public PeriodoOcupadoValidator createFromParcel(Parcel in) {
            return new PeriodoOcupadoValidator(in);
        }

        @Override
        public PeriodoOcupadoValidator[] newArray(int size) {
            return new PeriodoOcupadoValidator[size];
        }
    };
}
