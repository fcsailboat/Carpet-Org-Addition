package boat.carpetorgaddition.command;

import boat.carpetorgaddition.wheel.ExperienceTransfer;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class XpTransferCommandTest {
    // 包私有级成员变量
    static final int MAX_TRANSFER_LEVEL = 21863;

    @Disabled
    @RepeatedTest(124)
    public void testTransferAll(@NonNull RepetitionInfo info) {
        int level = (int) Math.pow(info.getCurrentRepetition(), 4);
        System.out.println("等级：" + level);
        MockPlayer player = new MockPlayer(level);
        BigInteger before = ExperienceTransfer.calculateTotalExperience(player.getExperienceLevel(), player.getPoint());
        System.out.println("转移前：" + before);
        player.clearExperience();
        transfer(player, before);
        BigInteger after = ExperienceTransfer.calculateTotalExperience(player.getExperienceLevel(), player.getPoint());
        System.out.println("转移后：" + after);
        BigInteger result = after.subtract(before);
        System.out.println("相差：" + result);
        String percentage = BigDecimal.valueOf(1).subtract(new BigDecimal(before).divide(new BigDecimal(after), 20, RoundingMode.HALF_UP)).abs().toPlainString();
        boolean correct = result.compareTo(BigInteger.ZERO) <= 0;
        System.out.println((correct ? "损失：" : "超出：") + percentage + "%");
        Assertions.assertTrue(correct);
    }

    @Test
    public void testCalculateTotalExperience() {
        Assertions.assertEquals(BigInteger.valueOf(7), ExperienceTransfer.calculateTotalExperience(1, 0));
        Assertions.assertEquals(BigInteger.valueOf(16), ExperienceTransfer.calculateTotalExperience(2, 0));
        Assertions.assertEquals(BigInteger.valueOf(27), ExperienceTransfer.calculateTotalExperience(3, 0));
        Assertions.assertEquals(BigInteger.valueOf(40), ExperienceTransfer.calculateTotalExperience(4, 0));
        Assertions.assertEquals(BigInteger.valueOf(55), ExperienceTransfer.calculateTotalExperience(5, 0));
        Assertions.assertEquals(BigInteger.valueOf(72), ExperienceTransfer.calculateTotalExperience(6, 0));
        Assertions.assertEquals(BigInteger.valueOf(91), ExperienceTransfer.calculateTotalExperience(7, 0));
        Assertions.assertEquals(BigInteger.valueOf(112), ExperienceTransfer.calculateTotalExperience(8, 0));
        Assertions.assertEquals(BigInteger.valueOf(135), ExperienceTransfer.calculateTotalExperience(9, 0));
        Assertions.assertEquals(BigInteger.valueOf(160), ExperienceTransfer.calculateTotalExperience(10, 0));
        Assertions.assertEquals(BigInteger.valueOf(187), ExperienceTransfer.calculateTotalExperience(11, 0));
        Assertions.assertEquals(BigInteger.valueOf(216), ExperienceTransfer.calculateTotalExperience(12, 0));
        Assertions.assertEquals(BigInteger.valueOf(247), ExperienceTransfer.calculateTotalExperience(13, 0));
        Assertions.assertEquals(BigInteger.valueOf(280), ExperienceTransfer.calculateTotalExperience(14, 0));
        Assertions.assertEquals(BigInteger.valueOf(315), ExperienceTransfer.calculateTotalExperience(15, 0));
        Assertions.assertEquals(BigInteger.valueOf(352), ExperienceTransfer.calculateTotalExperience(16, 0));
        Assertions.assertEquals(BigInteger.valueOf(394), ExperienceTransfer.calculateTotalExperience(17, 0));
        Assertions.assertEquals(BigInteger.valueOf(441), ExperienceTransfer.calculateTotalExperience(18, 0));
        Assertions.assertEquals(BigInteger.valueOf(493), ExperienceTransfer.calculateTotalExperience(19, 0));
        Assertions.assertEquals(BigInteger.valueOf(550), ExperienceTransfer.calculateTotalExperience(20, 0));
        Assertions.assertEquals(BigInteger.valueOf(612), ExperienceTransfer.calculateTotalExperience(21, 0));
        Assertions.assertEquals(BigInteger.valueOf(679), ExperienceTransfer.calculateTotalExperience(22, 0));
        Assertions.assertEquals(BigInteger.valueOf(751), ExperienceTransfer.calculateTotalExperience(23, 0));
        Assertions.assertEquals(BigInteger.valueOf(828), ExperienceTransfer.calculateTotalExperience(24, 0));
        Assertions.assertEquals(BigInteger.valueOf(910), ExperienceTransfer.calculateTotalExperience(25, 0));
        Assertions.assertEquals(BigInteger.valueOf(997), ExperienceTransfer.calculateTotalExperience(26, 0));
        Assertions.assertEquals(BigInteger.valueOf(1089), ExperienceTransfer.calculateTotalExperience(27, 0));
        Assertions.assertEquals(BigInteger.valueOf(1186), ExperienceTransfer.calculateTotalExperience(28, 0));
        Assertions.assertEquals(BigInteger.valueOf(1288), ExperienceTransfer.calculateTotalExperience(29, 0));
        Assertions.assertEquals(BigInteger.valueOf(1395), ExperienceTransfer.calculateTotalExperience(30, 0));
        Assertions.assertEquals(BigInteger.valueOf(1507), ExperienceTransfer.calculateTotalExperience(31, 0));
        Assertions.assertEquals(BigInteger.valueOf(1628), ExperienceTransfer.calculateTotalExperience(32, 0));
        Assertions.assertEquals(BigInteger.valueOf(1758), ExperienceTransfer.calculateTotalExperience(33, 0));
        Assertions.assertEquals(BigInteger.valueOf(1897), ExperienceTransfer.calculateTotalExperience(34, 0));
        Assertions.assertEquals(BigInteger.valueOf(2045), ExperienceTransfer.calculateTotalExperience(35, 0));
        Assertions.assertEquals(BigInteger.valueOf(2202), ExperienceTransfer.calculateTotalExperience(36, 0));
        Assertions.assertEquals(BigInteger.valueOf(2368), ExperienceTransfer.calculateTotalExperience(37, 0));
        Assertions.assertEquals(BigInteger.valueOf(2543), ExperienceTransfer.calculateTotalExperience(38, 0));
        Assertions.assertEquals(BigInteger.valueOf(2727), ExperienceTransfer.calculateTotalExperience(39, 0));
        Assertions.assertEquals(BigInteger.valueOf(2920), ExperienceTransfer.calculateTotalExperience(40, 0));
    }

    @Test
    public void testCalculateUpgradeExperience() {
        Assertions.assertEquals(BigInteger.valueOf(7), ExperienceTransfer.calculateUpgradeExperience(0, 1));
        Assertions.assertEquals(BigInteger.valueOf(64), ExperienceTransfer.calculateUpgradeExperience(3, 7));
        Assertions.assertEquals(BigInteger.valueOf(682), ExperienceTransfer.calculateUpgradeExperience(15, 26));
        Assertions.assertEquals(BigInteger.valueOf(9702), ExperienceTransfer.calculateUpgradeExperience(20, 64));
        Assertions.assertEquals(BigInteger.valueOf(30963), ExperienceTransfer.calculateUpgradeExperience(1, 100));
        Assertions.assertEquals(BigInteger.valueOf(358470), ExperienceTransfer.calculateUpgradeExperience(0, 300));
        Assertions.assertEquals(BigInteger.valueOf(1740870), ExperienceTransfer.calculateUpgradeExperience(20, 640));
        Assertions.assertEquals(BigInteger.valueOf(4308750), ExperienceTransfer.calculateUpgradeExperience(100, 1000));
        Assertions.assertEquals(BigInteger.valueOf(444037500), ExperienceTransfer.calculateUpgradeExperience(1000, 10000));
        Assertions.assertEquals(BigInteger.valueOf(2147407943), ExperienceTransfer.calculateUpgradeExperience(0, 21863));
        Assertions.assertEquals(BigInteger.valueOf(4499837502220L), ExperienceTransfer.calculateUpgradeExperience(0, 1000000));
        Assertions.assertEquals(BigInteger.valueOf(256204778204999070L), ExperienceTransfer.calculateUpgradeExperience(0, 238609312));
        Assertions.assertEquals(BigInteger.valueOf(-30963), ExperienceTransfer.calculateUpgradeExperience(100, 1));
        Assertions.assertEquals(BigInteger.valueOf(-4339720), ExperienceTransfer.calculateUpgradeExperience(1000, 0));
        Assertions.assertThrows(ArithmeticException.class, () -> ExperienceTransfer.calculateUpgradeExperience(0, 238609313));
    }

    private void transfer(MockPlayer player, BigInteger experience) {
        BigInteger maxValue = BigInteger.valueOf(Integer.MAX_VALUE);
        if (experience.compareTo(BigInteger.valueOf(10000000000L)) > 0) {
            experience = new BigDecimal(experience).multiply(new BigDecimal("0.9999999")).toBigInteger();
        } else if (experience.compareTo(BigInteger.valueOf(100000000000L)) > 0) {
            experience = new BigDecimal(experience).multiply(new BigDecimal("0.99999999")).toBigInteger();
        }
        while (experience.compareTo(maxValue) >= 0) {
            experience = experience.subtract(maxValue);
            player.addExperience(Integer.MAX_VALUE);
        }
        player.addExperience(experience.intValue());
    }
}
