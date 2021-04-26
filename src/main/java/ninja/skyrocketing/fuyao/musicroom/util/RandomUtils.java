package ninja.skyrocketing.fuyao.musicroom.util;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author skyrocketing Hong
 * @create 2020-08-07 14:18
 */
public class RandomUtils {
    private static Random getRandom() {
        return RandomHolderEnum.HOLDER.random;
    }

    /**
     * 生成一个<n的随机正整数
     *
     * @param n
     * @return
     */
    public static int getRandNumber(int n) {
        Random random = new Random();
        return random.nextInt(n);
    }

    private enum RandomHolderEnum {
        HOLDER;
        private final Random random;

        RandomHolderEnum() {
            random = ThreadLocalRandom.current();
        }
    }
}
