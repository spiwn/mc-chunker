package org.iz.cs.chunker.minecraft.impl;

import org.iz.cs.chunker.minecraft.Behavior;
import org.iz.cs.chunker.minecraft.Constants;

public interface GetDedicatedServerClassName {

    public static class I_1_15_2 extends Behavior<Void, String> {

        @Override
        public String apply(Void t) {
            return mapping.getClassName(Constants.DEDICATED_SERVER_CN);
        }
    }

}
