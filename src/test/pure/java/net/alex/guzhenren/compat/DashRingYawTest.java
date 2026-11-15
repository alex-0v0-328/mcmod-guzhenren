package net.alex.guzhenren.compat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 冲刺环偏航与冲刺动作：环随实际运动方向，非 payload 带的 Epic Fight 模型偏航；客户端已把横移、斜向角（±45° / ±90°）烘
 * 进模型偏航（{@code ClientEvents}），只剩后撤闪避动画与朝向相反需修正，故偏移不取水平输入，斜向再加 90° 即重复计算；同
 * 组轴选动作（{@link EpicFightIntegration#getDashClip}）：后冲撞、前冲撞、纯横移左右侧；加载
 * {@link EpicFightIntegration} 即校验其 Epic Fight 字段类型，故 pureTest 带 Epic Fight 的 jar。
 */

public class DashRingYawTest {

    @Test
    public void forwardAndSidewaysRideTheModelYaw() {
        assertEquals(0.0F, EpicFightIntegration.getRingYawOffset(1), "forward dash: model yaw is the motion");
        assertEquals(0.0F, EpicFightIntegration.getRingYawOffset(0), "sideways dash: model yaw is the motion");
    }

    @Test
    public void everyBackwardCombinationFlips180() {
        assertEquals(180.0F, EpicFightIntegration.getRingYawOffset(-1),
                "backward animation moves opposite to the model facing, diagonals included");
    }

    @Test
    public void theClipFollowsTheKeyboardAxes() {
        for (int horizontal = -1; horizontal <= 1; horizontal++) {
            assertEquals(EpicFightIntegration.DashClip.BACKWARD, EpicFightIntegration.getDashClip(-1, horizontal),
                    "every backward combination is the back ram");
            assertEquals(EpicFightIntegration.DashClip.FORWARD, EpicFightIntegration.getDashClip(1, horizontal),
                    "every forward combination is the forward ram");
        }
        assertEquals(EpicFightIntegration.DashClip.LEFT, EpicFightIntegration.getDashClip(0, 1),
                "the payload's horizontal is 1 for left");
        assertEquals(EpicFightIntegration.DashClip.RIGHT, EpicFightIntegration.getDashClip(0, -1));
    }
}
