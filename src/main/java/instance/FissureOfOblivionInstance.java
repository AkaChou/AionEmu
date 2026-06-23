/*
 * This file is part of Encom.
 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package instance;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.commons.network.util.ThreadPoolManager;

import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.FissureOfOblivionReward;
import com.aionemu.gameserver.model.instance.playerreward.FissureOfOblivionPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import javolution.util.*;

import java.util.*;
import java.util.concurrent.Future;

/****/
/** Author (Encom)
/** Source: https://www.youtube.com/watch?v=hQOGMAkf5ME#t=18.320136
/****/

@InstanceID(302100000)
public class FissureOfOblivionInstance extends GeneralInstanceHandler {

    private int rank;
    private long startTime;
    private Future<?> timerPrepare;
    private Future<?> timerInstance;
    
    private int[] killCounters = new int[10];
    
    private boolean isInstanceDestroyed;
    private Map<Integer, StaticDoor> doors;
    private FissureOfOblivionReward instanceReward;
    
    private int prepareTimerSeconds = 60000;
    private int instanceTimerSeconds = 1800000;
    private final FastList<Future<?>> oblivionTask = FastList.newInstance();

    private boolean spawned = false;

    private static class SpawnPoint {
        float x, y, z;
        byte heading;
        int delay;
        String walkerId;
        
        SpawnPoint(float x, float y, float z, byte heading) {
            this(x, y, z, heading, 0, null);
        }
        
        SpawnPoint(float x, float y, float z, byte heading, int delay) {
            this(x, y, z, heading, delay, null);
        }
        
        SpawnPoint(float x, float y, float z, byte heading, int delay, String walkerId) {
            this.x = x; this.y = y; this.z = z; this.heading = heading;
            this.delay = delay; this.walkerId = walkerId;
        }
    }
    
    private static final int[][] NPC_IDS = {
        {244470, 244471, 244472, 244473},
        {244511, 244512, 244513, 244514},
        {244552, 244553, 244554, 244555},
        {244593, 244594, 244595, 244596},
        {244634, 244635, 244636, 244637},
        {244675, 244676, 244677, 244678},
        {244716, 244717, 244718, 244719},
        {244757, 244758, 244759, 244760},
        {244798, 244799, 244800, 244801},
        {244839, 244840, 244841, 244842} 
    };
    
    private static final int[][] NPC_IDS_VRITRA = {
        {244474, 244475, 244476, 244477},
        {244515, 244516, 244517, 244518},
        {244556, 244557, 244558, 244559},
        {244597, 244598, 244599, 244600},
        {244638, 244639, 244640, 244641},
        {244679, 244680, 244681, 244682},
        {244720, 244721, 244722, 244723},
        {244761, 244762, 244763, 244764},
        {244802, 244803, 244804, 244805},
        {244843, 244844, 244845, 244846} 
    };
    
    private static final int[][] NPC_IDS_TIAMAT = {
        {244478, 244479, 244480, 244481},
        {244519, 244520, 244521, 244522},
        {244560, 244561, 244562, 244563},
        {244601, 244602, 244603, 244604},
        {244642, 244643, 244644, 244645},
        {244683, 244684, 244685, 244686},
        {244724, 244725, 244726, 244727},
        {244765, 244766, 244767, 244768},
        {244806, 244807, 244808, 244809},
        {244847, 244848, 244849, 244850} 
    };
    
    private static final int[][] NPC_IDS_ERESH_GUARD = {
        {244458, 244459, 244460, 244461},
        {244499, 244500, 244501, 244502},
        {244540, 244541, 244542, 244543},
        {244581, 244582, 244583, 244584},
        {244622, 244623, 244624, 244625},
        {244663, 244664, 244665, 244666},
        {244704, 244705, 244706, 244707},
        {244745, 244746, 244747, 244748},
        {244786, 244787, 244788, 244789},
        {244827, 244828, 244829, 244830} 
    };
    
    private static final int[][] NPC_IDS_VRITRA_GUARD = {
        {244462, 244463, 244464, 244465},
        {244503, 244504, 244505, 244506},
        {244544, 244545, 244546, 244547},
        {244585, 244586, 244587, 244588},
        {244626, 244627, 244628, 244629},
        {244667, 244668, 244669, 244670},
        {244708, 244709, 244710, 244711},
        {244749, 244750, 244751, 244752},
        {244790, 244791, 244792, 244793},
        {244831, 244832, 244833, 244834} 
    };
    
    private static final int[][] NPC_IDS_TIAMAT_GUARD = {
        {244466, 244467, 244468, 244469},
        {244507, 244508, 244509, 244510},
        {244548, 244549, 244550, 244551},
        {244589, 244590, 244591, 244592},
        {244630, 244631, 244632, 244633},
        {244671, 244672, 244673, 244674},
        {244712, 244713, 244714, 244715},
        {244753, 244754, 244755, 244756},
        {244794, 244795, 244796, 244797},
        {244835, 244836, 244837, 244838} 
    };
    
    private static final int[][] NPC_IDS_DEVA_GUARD = {
        {244485, 244486, 244487, 244488},
        {244526, 244527, 244528, 244529},
        {244567, 244568, 244569, 244570},
        {244608, 244609, 244610, 244611},
        {244649, 244650, 244651, 244652},
        {244690, 244691, 244692, 244693},
        {244731, 244732, 244733, 244734},
        {244772, 244773, 244774, 244775},
        {244813, 244814, 244815, 244816},
        {244854, 244855, 244856, 244857} 
    };
    
    private static final int[][] NPC_IDS_ERESH_STUMBLE = {
        {244864, 244865, 244866, 244867},
        {244895, 244896, 244897, 244898},
        {244926, 244927, 244928, 244929},
        {244957, 244958, 244959, 244960},
        {244988, 244989, 244990, 244991},
        {245019, 245020, 245021, 245022},
        {245050, 245051, 245052, 245053},
        {245081, 245082, 245083, 245084},
        {245112, 245113, 245114, 245115},
        {245143, 245144, 245145, 245146} 
    };
    
    private static final int[][] NPC_IDS_VRITRA_STUMBLE = {
        {244868, 244869, 244870, 244871},
        {244899, 244900, 244901, 244902},
        {244930, 244931, 244932, 244933},
        {244961, 244962, 244963, 244964},
        {244992, 244993, 244994, 244995},
        {245023, 245024, 245025, 245026},
        {245054, 245055, 245056, 245057},
        {245085, 245086, 245087, 245088},
        {245116, 245117, 245118, 245119},
        {245147, 245148, 245149, 245150} 
    };
    
    private static final int[][] NPC_IDS_TIAMAT_STUMBLE = {
        {244872, 244873, 244874, 244875},
        {244903, 244904, 244905, 244906},
        {244934, 244935, 244936, 244937},
        {244965, 244966, 244967, 244968},
        {244996, 244997, 244998, 244999},
        {245027, 245028, 245029, 245030},
        {245058, 245059, 245060, 245061},
        {245089, 245090, 245091, 245092},
        {245120, 245121, 245122, 245123},
        {245151, 245152, 245153, 245154} 
    };
    
    private static final int[][] NPC_IDS_DEVA_STUMBLE = {
        {244876, 244877, 244878, 244879},
        {244907, 244908, 244909, 244910},
        {244938, 244939, 244940, 244941},
        {244969, 244970, 244971, 244972},
        {245000, 245001, 245002, 245003},
        {245031, 245032, 245033, 245034},
        {245062, 245063, 245064, 245065},
        {245093, 245094, 245095, 245096},
        {245124, 245125, 245126, 245127},
        {245155, 245156, 245157, 245158} 
    };
    
    private static final int[][] NPC_IDS_ERESH_RA = {
        {245697, 245698, 245699},
        {245703, 245704, 245705},
        {245709, 245710, 245711},
        {245715, 245716, 245717},
        {245721, 245722, 245723},
        {245727, 245728, 245729},
        {245733, 245734, 245735},
        {245739, 245740, 245741},
        {245745, 245746, 245747},
        {245751, 245752, 245753} 
    };
    
    private static final int[][] NPC_IDS_DROP_DRAGON = {
        {244892, 244893, 244894},
        {244923, 244924, 244925},
        {244954, 244955, 244956},
        {244985, 244986, 244987},
        {245016, 245017, 245018},
        {245047, 245048, 245049},
        {245078, 245079, 245080},
        {245109, 245110, 245111},
        {245140, 245141, 245142},
        {245171, 245172, 245173} 
    };
    
    private static final int[] NPC_IDS_BONUS_MONSTER = {
        246200, 246201, 246202, 246203,
        246204, 246205, 246206, 246207,
        246208, 246209, 246210, 246211,
        246212, 246213, 246214, 246215,
        246216, 246217, 246218, 246219,
        246220, 246221, 246222, 246223,
        246224, 246225, 246226, 246227,
        246228, 246229, 246230, 246231,
        246232, 246233, 246234, 246235,
        246236, 246237, 246238, 246239 
    };
    
    private static final int[][] NPC_IDS_WARP = {
        {245577, 245578, 245579, 245580, 245581, 245582, 245583, 245584, 245585, 245586, 245587, 245588},
        {245589, 245590, 245591, 245592, 245593, 245594, 245595, 245596, 245597, 245598, 245599, 245600},
        {245601, 245602, 245603, 245604, 245605, 245606, 245607, 245608, 245609, 245610, 245611, 245612},
        {245613, 245614, 245615, 245616, 245617, 245618, 245619, 245620, 245621, 245622, 245623, 245624},
        {245625, 245626, 245627, 245628, 245629, 245630, 245631, 245632, 245633, 245634, 245635, 245636},
        {245637, 245638, 245639, 245640, 245641, 245642, 245643, 245644, 245645, 245646, 245647, 245648},
        {245649, 245650, 245651, 245652, 245653, 245654, 245655, 245656, 245657, 245658, 245659, 245660},
        {245661, 245662, 245663, 245664, 245665, 245666, 245667, 245668, 245669, 245670, 245671, 245672},
        {245673, 245674, 245675, 245676, 245677, 245678, 245679, 245680, 245681, 245682, 245683, 245684},
        {245685, 245686, 245687, 245688, 245689, 245690, 245691, 245692, 245693, 245694, 245695, 245696} 
    };
    
    private static final SpawnPoint[] SPAWN_HIGH_MAIN = {
        new SpawnPoint(587.9679f, 620.0452f, 331.7278f, (byte)15),
        new SpawnPoint(574.8497f, 668.79987f, 306.13416f, (byte)78),
        new SpawnPoint(579.0743f, 665.09393f, 306.088f, (byte)77),
        new SpawnPoint(577.0f, 667.0f, 306.1446f, (byte)77),
        new SpawnPoint(592.84674f, 627.5482f, 331.72772f, (byte)30),
        new SpawnPoint(575.2141f, 664.8451f, 306.0879f, (byte)78),
        new SpawnPoint(590.92944f, 620.8617f, 331.72787f, (byte)21),
        new SpawnPoint(587.0f, 626.0f, 331.80182f, (byte)21),
        new SpawnPoint(559.017f, 619.6899f, 326.24957f, (byte)1),
        new SpawnPoint(584.7248f, 623.8249f, 331.80182f, (byte)10),
        new SpawnPoint(553.0f, 618.0f, 326.2498f, (byte)10),
        new SpawnPoint(578.6354f, 669.4132f, 306.0881f, (byte)79),
        new SpawnPoint(552.9948f, 622.4288f, 326.2497f, (byte)0),
        new SpawnPoint(557.20746f, 616.5142f, 326.2497f, (byte)0),
        new SpawnPoint(558.10596f, 623.51215f, 326.3014f, (byte)116),
        new SpawnPoint(467.57324f, 477.38986f, 345.70047f, (byte)84),
        new SpawnPoint(482.65543f, 498.34192f, 342.22174f, (byte)87)
    };
    
    private static final SpawnPoint[] SPAWN_GUARD_MAIN = {
        new SpawnPoint(796.0f, 502.0f, 340.625f, (byte)0),
        new SpawnPoint(854.7336f, 485.80002f, 348.83334f, (byte)102),
        new SpawnPoint(799.1453f, 507.0006f, 340.83063f, (byte)119),
        new SpawnPoint(796.0f, 518.0f, 340.0f, (byte)119),
        new SpawnPoint(862.7976f, 493.89987f, 349.12473f, (byte)98),
        new SpawnPoint(608.63324f, 559.0327f, 352.1944f, (byte)112),
        new SpawnPoint(593.5001f, 674.01624f, 350.85538f, (byte)0),
        new SpawnPoint(537.9779f, 494.51947f, 322.0f, (byte)30),
        new SpawnPoint(851.19464f, 483.77264f, 348.9846f, (byte)103),
        new SpawnPoint(536.33435f, 487.47772f, 322.0f, (byte)30),
        new SpawnPoint(604.0555f, 636.776f, 352.5063f, (byte)94),
        new SpawnPoint(530.9891f, 501.1546f, 321.87363f, (byte)31),
        new SpawnPoint(799.2242f, 521.97736f, 340.47952f, (byte)118),
        new SpawnPoint(530.97894f, 492.33932f, 322.0f, (byte)31),
        new SpawnPoint(858.3547f, 491.6155f, 348.8165f, (byte)104),
        new SpawnPoint(607.52203f, 555.99097f, 352.1944f, (byte)119),
        new SpawnPoint(544.9966f, 497.9999f, 322.0f, (byte)30),
        new SpawnPoint(527.03064f, 492.47556f, 322.0f, (byte)31),
        new SpawnPoint(640.6056f, 514.0086f, 339.61542f, (byte)1),
        new SpawnPoint(632.79755f, 516.58496f, 339.61548f, (byte)1),
        new SpawnPoint(633.14124f, 511.15356f, 339.61548f, (byte)2),
        new SpawnPoint(632.80096f, 522.824f, 339.61545f, (byte)113),
        new SpawnPoint(534.67413f, 483.98718f, 322.0f, (byte)30),
        new SpawnPoint(637.7931f, 525.1838f, 339.61545f, (byte)107),
        new SpawnPoint(537.44684f, 507.10223f, 322.0f, (byte)30),
        new SpawnPoint(790.6567f, 503.19193f, 339.64206f, (byte)2),
        new SpawnPoint(612.65625f, 645.03973f, 352.49344f, (byte)91),
        new SpawnPoint(606.4433f, 558.1034f, 352.1944f, (byte)113),
        new SpawnPoint(794.0673f, 521.59106f, 339.63763f, (byte)115),
        new SpawnPoint(790.0854f, 507.5921f, 339.28082f, (byte)3),
        new SpawnPoint(609.1728f, 561.4424f, 352.20877f, (byte)108),
        new SpawnPoint(546.9983f, 509.095f, 321.93762f, (byte)30),
        new SpawnPoint(528.00977f, 501.40976f, 321.6244f, (byte)30),
        new SpawnPoint(791.13544f, 525.8589f, 339.43713f, (byte)0),
        new SpawnPoint(526.723f, 485.72604f, 321.79556f, (byte)31),
        new SpawnPoint(548.0f, 489.0f, 321.5f, (byte)31),
        new SpawnPoint(792.1176f, 505.79953f, 339.52722f, (byte)3),
        new SpawnPoint(541.7361f, 485.9917f, 322.0f, (byte)30),
        new SpawnPoint(543.99554f, 490.13123f, 322.0f, (byte)30),
        new SpawnPoint(791.39966f, 518.47125f, 339.42496f, (byte)0),
        new SpawnPoint(641.4397f, 520.0093f, 339.61542f, (byte)119)
    };
    
    private static final SpawnPoint[] SPAWN_DEVA_GUARD = {
        new SpawnPoint(543.9115f, 491.4599f, 322.04422f, (byte)91),
        new SpawnPoint(547.9682f, 490.34494f, 321.50793f, (byte)90),
        new SpawnPoint(534.53534f, 485.35455f, 322.0f, (byte)89),
        new SpawnPoint(541.57935f, 487.14307f, 322.0f, (byte)89),
        new SpawnPoint(516.4117f, 585.5329f, 321.97427f, (byte)90),
        new SpawnPoint(499.82422f, 585.81537f, 322.02252f, (byte)92),
        new SpawnPoint(501.61078f, 555.0141f, 321.84106f, (byte)12)
    };
    
    private static final SpawnPoint[] SPAWN_DEVA_GUARD_AS = {
        new SpawnPoint(855.63855f, 484.74774f, 349.08722f, (byte)45),
        new SpawnPoint(852.21844f, 482.80756f, 349.41986f, (byte)44),
        new SpawnPoint(864.0118f, 492.74384f, 349.56552f, (byte)46),
        new SpawnPoint(508.17993f, 587.8108f, 322.56973f, (byte)90),
        new SpawnPoint(526.8684f, 493.7475f, 322.0f, (byte)91),
        new SpawnPoint(537.37225f, 508.5786f, 322.0f, (byte)90),
        new SpawnPoint(512.8187f, 590.4575f, 322.56216f, (byte)90),
        new SpawnPoint(504.50745f, 590.3867f, 322.56216f, (byte)90),
        new SpawnPoint(482.3006f, 496.94232f, 342.22174f, (byte)27),
        new SpawnPoint(594.4832f, 673.60126f, 350.85538f, (byte)54),
        new SpawnPoint(634.298f, 511.2995f, 339.61545f, (byte)61),
        new SpawnPoint(530.982f, 493.62543f, 322.0f, (byte)90),
        new SpawnPoint(641.9954f, 514.2143f, 339.61542f, (byte)61),
        new SpawnPoint(505.45636f, 584.90186f, 322.0f, (byte)90),
        new SpawnPoint(510.66394f, 584.80176f, 322.0f, (byte)90),
        new SpawnPoint(859.2291f, 490.53555f, 348.98047f, (byte)45),
        new SpawnPoint(604.1567f, 635.69995f, 352.53815f, (byte)30),
        new SpawnPoint(467.0f, 476.0f, 345.70047f, (byte)32),
        new SpawnPoint(536.2914f, 488.65588f, 322.0f, (byte)90),
        new SpawnPoint(639.6124f, 525.6597f, 339.61542f, (byte)62),
        new SpawnPoint(885.47394f, 464.9668f, 351.0f, (byte)66),
        new SpawnPoint(642.88293f, 520.17224f, 339.61542f, (byte)60),
        new SpawnPoint(546.91925f, 510.66574f, 321.94254f, (byte)91),
        new SpawnPoint(527.7633f, 502.87167f, 321.6398f, (byte)91),
        new SpawnPoint(885.5619f, 456.29126f, 351.02737f, (byte)55)
    };
    
    private static final SpawnPoint[] SPAWN_DEVA_GUARD_WI = {
        new SpawnPoint(513.0236f, 593.86285f, 322.56216f, (byte)90),
        new SpawnPoint(501.3256f, 593.69885f, 322.56216f, (byte)90),
        new SpawnPoint(633.75323f, 516.50934f, 339.61545f, (byte)61),
        new SpawnPoint(544.93945f, 499.1886f, 322.0f, (byte)91),
        new SpawnPoint(530.8947f, 502.38684f, 321.86185f, (byte)91),
        new SpawnPoint(634.5065f, 522.0903f, 339.61545f, (byte)53),
        new SpawnPoint(885.382f, 460.77805f, 351.0f, (byte)60),
        new SpawnPoint(612.8048f, 643.80194f, 352.46414f, (byte)30),
        new SpawnPoint(538.00024f, 495.57394f, 322.0f, (byte)90),
        new SpawnPoint(526.9178f, 486.92227f, 321.87527f, (byte)85)
    };
    
    private static final SpawnPoint[] SPAWN_DEVA_GUARD_RA = {
        new SpawnPoint(529.87f, 545.5564f, 321.85876f, (byte)95),
        new SpawnPoint(526.1612f, 545.1311f, 321.58078f, (byte)88),
        new SpawnPoint(546.40234f, 523.6142f, 321.97485f, (byte)87),
        new SpawnPoint(549.9647f, 524.9884f, 321.9978f, (byte)95),
        new SpawnPoint(520.9293f, 586.19006f, 321.94193f, (byte)95),
        new SpawnPoint(495.259f, 586.9457f, 322.0659f, (byte)81),
        new SpawnPoint(505.0709f, 552.7453f, 322.0f, (byte)28)
    };
    
    private static final SpawnPoint[] SPAWN_STUMBLE_MAIN = {
        new SpawnPoint(856.45654f, 530.1012f, 346.1631f, (byte)86),
        new SpawnPoint(504.76083f, 513.9777f, 339.63126f, (byte)61),
        new SpawnPoint(809.2487f, 476.4447f, 340.87885f, (byte)29)
    };
    
    private static final SpawnPoint[] SPAWN_DEVA_STUMBLE = {
        new SpawnPoint(677.0f, 516.0f, 338.24844f, (byte)43),
        new SpawnPoint(505.28098f, 648.23944f, 317.08282f, (byte)113),
        new SpawnPoint(507.24606f, 628.40027f, 318.4649f, (byte)61),
        new SpawnPoint(504.39532f, 615.5569f, 320.3689f, (byte)61),
        new SpawnPoint(504.10117f, 601.42255f, 322.4643f, (byte)60),
        new SpawnPoint(513.7057f, 643.4998f, 317.10632f, (byte)11),
        new SpawnPoint(511.02466f, 614.0008f, 320.5996f, (byte)0),
        new SpawnPoint(503.81223f, 640.00806f, 317.11395f, (byte)40),
        new SpawnPoint(531.7686f, 642.5042f, 317.08282f, (byte)112),
        new SpawnPoint(522.0014f, 648.79095f, 317.08282f, (byte)55)
    };
    
    private static final SpawnPoint[] SPAWN_DROP_DRAGON = {
        new SpawnPoint(622.6814f, 551.9144f, 346.06897f, (byte)105),
        new SpawnPoint(607.8992f, 674.06934f, 352.29062f, (byte)90)
    };
    
    private static final SpawnPoint[] SPAWN_BONUS_MONSTER = {
        new SpawnPoint(761.54095f, 562.17f, 341.0512f, (byte)90),
        new SpawnPoint(476.94467f, 549.22363f, 345.6048f, (byte)90),
        new SpawnPoint(609.2811f, 707.6352f, 355.10846f, (byte)93)
    };
    
    private static final SpawnPoint[] SPAWN_RANGER = {
        new SpawnPoint(543.0933f, 557.8653f, 322.0f, (byte)70),
        new SpawnPoint(550.4571f, 546.09937f, 322.0f, (byte)61),
        new SpawnPoint(550.2217f, 539.29755f, 322.0f, (byte)59),
        new SpawnPoint(543.0761f, 534.39087f, 322.0f, (byte)43),
        new SpawnPoint(534.18945f, 534.63385f, 321.875f, (byte)21),
        new SpawnPoint(522.56396f, 467.8957f, 322.0f, (byte)37),
        new SpawnPoint(527.70734f, 475.95486f, 322.0f, (byte)7),
        new SpawnPoint(509.6404f, 476.19836f, 322.0f, (byte)118),
        new SpawnPoint(501.61966f, 467.60513f, 322.0f, (byte)5),
        new SpawnPoint(503.2415f, 446.9365f, 321.8276f, (byte)18),
        new SpawnPoint(542.8608f, 472.11404f, 322.0f, (byte)51),
        new SpawnPoint(539.9602f, 467.72766f, 322.0f, (byte)50),
        new SpawnPoint(516.02167f, 453.723f, 321.875f, (byte)60),
        new SpawnPoint(466.30823f, 509.6083f, 342.1901f, (byte)0),
        new SpawnPoint(466.1356f, 516.46765f, 342.1901f, (byte)0),
        new SpawnPoint(468.7195f, 513.0958f, 342.1901f, (byte)0),
        new SpawnPoint(446.91077f, 509.62943f, 342.2713f, (byte)1),
        new SpawnPoint(446.86057f, 517.2843f, 342.27173f, (byte)0),
        new SpawnPoint(449.71277f, 513.23883f, 342.24753f, (byte)0),
        new SpawnPoint(427.54434f, 509.3018f, 342.43564f, (byte)0),
        new SpawnPoint(427.4629f, 517.19116f, 342.43634f, (byte)0),
        new SpawnPoint(430.6331f, 513.1085f, 342.40942f, (byte)0),
        new SpawnPoint(483.40283f, 475.9793f, 345.70047f, (byte)81),
        new SpawnPoint(469.33865f, 454.01733f, 345.70047f, (byte)4),
        new SpawnPoint(482.78824f, 526.4342f, 342.234f, (byte)78),
        new SpawnPoint(502.18372f, 508.00098f, 339.63123f, (byte)39),
        new SpawnPoint(497.84283f, 521.77655f, 339.63815f, (byte)77),
        new SpawnPoint(508.98663f, 527.061f, 339.63126f, (byte)76),
        new SpawnPoint(502.5523f, 649.1053f, 317.08282f, (byte)109),
        new SpawnPoint(522.4196f, 650.3398f, 317.08282f, (byte)99),
        new SpawnPoint(540.84265f, 652.0684f, 316.16275f, (byte)100),
        new SpawnPoint(540.5331f, 637.4856f, 316.17746f, (byte)17),
        new SpawnPoint(514.6148f, 641.9673f, 317.10098f, (byte)33),
        new SpawnPoint(539.03076f, 536.819f, 322.0f, (byte)30)
    };
    
    private static final SpawnPoint[] SPAWN_WARP = {
        new SpawnPoint(725.0f, 515.0f, 338.24844f, (byte)1, 0),
        new SpawnPoint(722.0f, 513.0f, 338.24844f, (byte)2, 2000),
        new SpawnPoint(722.0f, 517.3f, 338.24844f, (byte)0, 2000),
        new SpawnPoint(725.0f, 502.0f, 338.24844f, (byte)2, 2000),
        new SpawnPoint(722.7f, 499.3f, 338.37372f, (byte)1, 2000),
        new SpawnPoint(722.0f, 503.0f, 338.24844f, (byte)2, 2000),
        new SpawnPoint(703.0f, 514.0f, 338.24844f, (byte)4, 2000),
        new SpawnPoint(700.0f, 516.0f, 338.24844f, (byte)2, 2000),
        new SpawnPoint(697.0f, 515.0f, 338.24844f, (byte)2, 2000),
        new SpawnPoint(703.0f, 503.0f, 338.24844f, (byte)2, 2000),
        new SpawnPoint(699.0f, 506.0f, 338.24844f, (byte)2, 2000),
        new SpawnPoint(696.0f, 504.0f, 338.24844f, (byte)2, 2000),
        new SpawnPoint(672.0f, 523.0f, 338.24844f, (byte)1, 2000),
        new SpawnPoint(669.0f, 520.0f, 338.24844f, (byte)2, 2000),
        new SpawnPoint(668.1f, 525.2f, 338.24844f, (byte)0, 2000),
        new SpawnPoint(664.9f, 520.7f, 338.24844f, (byte)115, 2000),
        new SpawnPoint(674.0f, 509.6f, 338.24844f, (byte)0, 2000),
        new SpawnPoint(671.0f, 512.0f, 338.24844f, (byte)2, 2000),
        new SpawnPoint(667.2f, 510.0f, 338.24844f, (byte)1, 2000)
    };
 
    private int getLevelIndex(Player player) {
        return player.getLevel() - 66;
    }
    
    private void spawnGroup(int[][] npcIds, SpawnPoint[] points, int level, int classIndex) {
        int idx = getLevelIndexByLevel(level);
        int npcId = npcIds[idx][classIndex];
        for (SpawnPoint point : points) {
            if (point.delay > 0) {
                sp(npcId, point.x, point.y, point.z, point.heading, point.delay);
            } else {
                spawn(npcId, point.x, point.y, point.z, point.heading);
            }
        }
    }
    
    private void spawnGroup(int[] npcIds, SpawnPoint[] points, int level, int offset) {
        int idx = getLevelIndexByLevel(level);
        int npcId = npcIds[idx * 4 + offset];
        for (SpawnPoint point : points) {
            spawn(npcId, point.x, point.y, point.z, point.heading);
        }
    }
    
    private int getLevelIndexByLevel(int level) {
        return level - 66;
    }

    protected FissureOfOblivionPlayerReward getPlayerReward(Integer object) {
        return (FissureOfOblivionPlayerReward) instanceReward.getPlayerReward(object);
    }
    
    protected void addPlayerReward(Player player) {
        instanceReward.addPlayerReward(new FissureOfOblivionPlayerReward(player.getObjectId()));
    }
    
    private boolean containPlayer(Integer object) {
        return instanceReward.containPlayer(object);
    }
    
    @Override
    public InstanceReward<?> getInstanceReward() {
        return instanceReward;
    }
    
    public void onDropRegistered(Npc npc) {
        Set<DropItem> dropItems = DropRegistrationService.getInstance().getCurrentDropMap().get(npc.getObjectId());
        int npcId = npc.getNpcId();
        if (npcId >= 246200 && npcId <= 246239) {
            switch (Rnd.get(1, 5)) {
                case 1: dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 188055607, 1)); break;
                case 2: dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 188055608, 1)); break;
                case 3: dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 188055609, 1)); break;
                case 4: dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 188055610, 1)); break;
                case 5: dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 188055611, 1)); break;
            }
            switch (Rnd.get(1, 5)) {
                case 1: dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 190080005, 3)); break;
                case 2: dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 190080006, 3)); break;
                case 3: dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 190080007, 3)); break;
                case 4: dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 190080008, 3)); break;
                case 5: dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 190200000, 50)); break;
            }
        }
        if ((npcId >= 244490 && npcId <= 244494) || (npcId >= 244531 && npcId <= 244535) ||
            (npcId >= 244572 && npcId <= 244576) || (npcId >= 244613 && npcId <= 244617) ||
            (npcId >= 244654 && npcId <= 244658) || (npcId >= 244695 && npcId <= 244699) ||
            (npcId >= 244736 && npcId <= 244740) || (npcId >= 244777 && npcId <= 244781) ||
            (npcId >= 244818 && npcId <= 244822) || (npcId >= 244859 && npcId <= 244863)) {
            switch (Rnd.get(1, 5)) {
                case 1: dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 190080005, 3)); break;
                case 2: dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 190080006, 3)); break;
                case 3: dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 190080007, 3)); break;
                case 4: dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 190080008, 3)); break;
                case 5: dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 190200000, 50)); break;
            }
        }
    }
    
    @Override
    public void onDie(Npc npc) {
        int points = 0;
        int npcId = npc.getNpcId();
        Player player = npc.getAggroList().getMostPlayerDamage();
          
        final int monsterLevel;
        if (npcId >= 244454 && npcId <= 244457) monsterLevel = 66;
        else if (npcId >= 244495 && npcId <= 244498) monsterLevel = 67;
        else if (npcId >= 244536 && npcId <= 244539) monsterLevel = 68;
        else if (npcId >= 244577 && npcId <= 244580) monsterLevel = 69;
        else if (npcId >= 244618 && npcId <= 244621) monsterLevel = 70;
        else if (npcId >= 244659 && npcId <= 244662) monsterLevel = 71;
        else if (npcId >= 244700 && npcId <= 244703) monsterLevel = 72;
        else if (npcId >= 244741 && npcId <= 244744) monsterLevel = 73;
        else if (npcId >= 244782 && npcId <= 244785) monsterLevel = 74;
        else if (npcId >= 244823 && npcId <= 244826) monsterLevel = 75;
        else monsterLevel = 0;
          
        if (monsterLevel > 0) {
             final int idx = getLevelIndexByLevel(monsterLevel);
             killCounters[idx]++;
               
             if (killCounters[idx] == 4) {
                  ThreadPoolManager.getInstance().schedule(new Runnable() {
                       @Override
                       public void run() {
                            int warpType = Rnd.get(1, 3);
                            switch (warpType) {
                                 case 1: IDTransformEreshWarp(monsterLevel); break;
                                 case 2: IDTransformVritraWarp(monsterLevel); break;
                                 case 3: IDTransformTiamatWarp(monsterLevel); break;
                            }
							killNpc(getNpcs(245402));
                            deleteNpc(245402);
                            spawn(245416, 855.54144f, 465.55255f, 351.57367f, (byte)0, 54);
                       }
                  }, 5000);
             }
             else if (killCounters[idx] == 8) {
                  ThreadPoolManager.getInstance().schedule(new Runnable() {
                       @Override
                       public void run() {
							killNpc(getNpcs(245403));
                            deleteNpc(245403);
                            spawn(245417, 594.41882f, 564.05542f, 352.56454f, (byte)0, 58);
                            spawn(245418, 522.48053f, 573.51971f, 321.80389f, (byte)0, 55);
                       }
                  }, 5000);
             }
             else if (killCounters[idx] == 12) {
                  ThreadPoolManager.getInstance().schedule(new Runnable() {
                       @Override
                       public void run() {
							killNpc(getNpcs(245404));
                            deleteNpc(245404);
                       }
                  }, 5000);
             }
             points = 250;
             despawnNpc(npc);
        }
          
        if ((npcId >= 244470 && npcId <= 244481) ||
             (npcId >= 244511 && npcId <= 244522) ||
             (npcId >= 244552 && npcId <= 244563) ||
             (npcId >= 244593 && npcId <= 244604) ||
             (npcId >= 244634 && npcId <= 244645) ||
             (npcId >= 244675 && npcId <= 244686) ||
             (npcId >= 244716 && npcId <= 244727) ||
             (npcId >= 244757 && npcId <= 244768) ||
             (npcId >= 244798 && npcId <= 244809) ||
             (npcId >= 244839 && npcId <= 244850) ||
             (npcId >= 245577 && npcId <= 245696)) {
             points = 250;
             despawnNpc(npc);
        }
          
        if ((npcId >= 244482 && npcId <= 244484) || (npcId >= 244523 && npcId <= 244525) ||
             (npcId >= 244564 && npcId <= 244566) || (npcId >= 244605 && npcId <= 244607) ||
             (npcId >= 244646 && npcId <= 244648) || (npcId >= 244687 && npcId <= 244689) ||
             (npcId >= 244728 && npcId <= 244730) || (npcId >= 244769 && npcId <= 244771) ||
             (npcId >= 244810 && npcId <= 244812) || (npcId >= 244851 && npcId <= 244853) ||
             (npcId >= 244892 && npcId <= 244894) || (npcId >= 244923 && npcId <= 244925) ||
             (npcId >= 244954 && npcId <= 244956) || (npcId >= 244985 && npcId <= 244987) ||
             (npcId >= 245016 && npcId <= 245018) || (npcId >= 245047 && npcId <= 245049) ||
             (npcId >= 245078 && npcId <= 245080) || (npcId >= 245109 && npcId <= 245111) ||
             (npcId >= 245140 && npcId <= 245142) || (npcId >= 245171 && npcId <= 245173)) {
             points = 550;
             despawnNpc(npc);
             doors.get(183).setOpen(true);
        }
          
        if ((npcId >= 244490 && npcId <= 244494) || (npcId >= 244531 && npcId <= 244535) ||
             (npcId >= 244572 && npcId <= 244576) || (npcId >= 244613 && npcId <= 244617) ||
             (npcId >= 244654 && npcId <= 244658) || (npcId >= 244695 && npcId <= 244699) ||
             (npcId >= 244736 && npcId <= 244740) || (npcId >= 244777 && npcId <= 244781) ||
             (npcId >= 244818 && npcId <= 244822) || (npcId >= 244859 && npcId <= 244863)) {
             points = 1500;
             despawnNpc(npc);
             deleteNpc(245405);
             ThreadPoolManager.getInstance().schedule(new Runnable() {
                  @Override
                  public void run() {
                       instance.doOnAllPlayers(new Visitor<Player>() {
                            @Override
                            public void visit(Player player) {
                                 stopInstance(player);
                            }
                       });
                  }
             }, 3000);
        }
          
        if (instanceReward.getInstanceScoreType().isStartProgress()) {
             instanceReward.addNpcKill();
             instanceReward.addPoints(points);
             sendPacket(npc.getObjectTemplate().getNameId(), points);
        }
    }
    
    private int getTime() {
        long result = (int) (System.currentTimeMillis() - startTime);
        return instanceTimerSeconds - (int) result;
    }
    
    private void sendPacket(final int nameId, final int point) {
        instance.doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player player) {
                if (nameId != 0) {
                    PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400237, new DescriptionId(nameId * 2 + 1), point));
                }
                PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(getTime(), instanceReward, null));
            }
        });
    }
    
    private int checkRank(int totalPoints) {
        if (totalPoints >= 23550) { //Rank S
            return 1;
        } else if (totalPoints >= 21200) { //Rank A
            return 2;
        } else if (totalPoints >= 17700) { //Rank B
            return 3;
        } else if (totalPoints >= 14100) { //Rank C
            return 4;
        } else if (totalPoints >= 9400) { //Rank D
            return 5;
        } else {
            return 6;
        }
    }
    
    protected void startInstanceTask() {
        oblivionTask.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                instance.doOnAllPlayers(new Visitor<Player>() {
                    @Override
                    public void visit(Player player) {
                        stopInstance(player);
                    }
                });
            }
        }, 1800000));
    }
    
    @Override
    public void onOpenDoor(Player player, int doorId) {
        if (doorId == 34) {
            startInstanceTask();
            doors.get(34).setOpen(true);
            sendMsgByRace(1403698, Race.PC_ALL, 5000);
            if ((timerPrepare != null) && (!timerPrepare.isDone() || !timerPrepare.isCancelled())) {
                startMainInstanceTimer();
            }
        }
    }
    
    @Override
    public void onEnterInstance(final Player player) {
        if (!instanceReward.containPlayer(player.getObjectId())) {
            addPlayerReward(player);
        }
        FissureOfOblivionPlayerReward playerReward = getPlayerReward(player.getObjectId());
        if (playerReward.isRewarded()) {
            doReward(player);
        }
        startPrepareTimer();
        if (!spawned) {
           spawnByPlayerLevel(player);
           spawned = true;
        }
        SkillEngine.getInstance().applyEffectDirectly(4831, player, player, 1800000 * 1);
    }
    
    private void startPrepareTimer() {
        if (timerPrepare == null) {
            timerPrepare = ThreadPoolManager.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    startMainInstanceTimer();
                }
            }, prepareTimerSeconds);
        }
        instance.doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player player) {
                PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(prepareTimerSeconds, instanceReward, null));
            }
        });
    }
    
    private void startMainInstanceTimer() {
        if (!timerPrepare.isDone()) {
            timerPrepare.cancel(false);
        }
        startTime = System.currentTimeMillis();
        instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
        sendPacket(0, 0);
    }
    
    protected void stopInstance(Player player) {
        stopInstanceTask();
        instanceReward.setRank(6);
        instanceReward.setRank(checkRank(instanceReward.getPoints()));
        instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
        doReward(player);
        sendPacket(0, 0);
    }
    
    @Override
    public void doReward(Player player) {
        FissureOfOblivionPlayerReward playerReward = getPlayerReward(player.getObjectId());
        if (!playerReward.isRewarded()) {
            playerReward.setRewarded();
            int oblivionRank = instanceReward.getRank();
            int amount = 0;
            switch (oblivionRank) {
                case 1: amount = 5; break;
                case 2: amount = 4; break;
                case 3: amount = 3; break;
                case 4: amount = 2; break;
                case 5: amount = 1; break;
            }
            if (amount > 0) {
                playerReward.setFrozenMarbleOfMemory(amount);
                ItemService.addItem(player, 186000448, amount);
            }
        }
    }
    
    @Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
        instanceReward = new FissureOfOblivionReward(mapId, instanceId);
        instanceReward.setInstanceScoreType(InstanceScoreType.PREPARING);
        doors = instance.getDoors();
    }
    
     private void spawnByPlayerLevel(Player player) {
          int level = player.getLevel();
          if (level < 66 || level > 75) return;
          
          int idx = getLevelIndexByLevel(level);

          int raType = Rnd.get(1, 3);
          int raNpcId = NPC_IDS_ERESH_RA[idx][raType - 1];
          for (SpawnPoint point : SPAWN_RANGER) {
               spawn(raNpcId, point.x, point.y, point.z, point.heading);
          }
          
          int dragonType = Rnd.get(1, 3);
          for (SpawnPoint point : SPAWN_DROP_DRAGON) {
               spawn(NPC_IDS_DROP_DRAGON[idx][dragonType - 1], point.x, point.y, point.z, point.heading);
          }
          
          int bonusType = Rnd.get(1, 4);
          int bonusNpcId = NPC_IDS_BONUS_MONSTER[idx * 4 + (bonusType - 1)];
          for (SpawnPoint point : SPAWN_BONUS_MONSTER) {
               spawn(bonusNpcId, point.x, point.y, point.z, point.heading);
          }
          
          for (SpawnPoint point : SPAWN_DEVA_GUARD) {
               spawn(NPC_IDS_DEVA_GUARD[idx][0], point.x, point.y, point.z, point.heading);
          }
          
          for (SpawnPoint point : SPAWN_DEVA_GUARD_AS) {
               spawn(NPC_IDS_DEVA_GUARD[idx][1], point.x, point.y, point.z, point.heading);
          }
          
          for (SpawnPoint point : SPAWN_DEVA_GUARD_WI) {
               spawn(NPC_IDS_DEVA_GUARD[idx][2], point.x, point.y, point.z, point.heading);
          }
          
          for (SpawnPoint point : SPAWN_DEVA_GUARD_RA) {
               spawn(NPC_IDS_DEVA_GUARD[idx][3], point.x, point.y, point.z, point.heading);
          }
          
          int stumbleType = Rnd.get(1, 3);
          int stumbleClass = Rnd.get(0, 3);
          int[] selectedStumble = null;
          if (stumbleType == 1) selectedStumble = NPC_IDS_ERESH_STUMBLE[idx];
          else if (stumbleType == 2) selectedStumble = NPC_IDS_VRITRA_STUMBLE[idx];
          else selectedStumble = NPC_IDS_TIAMAT_STUMBLE[idx];
          
          for (SpawnPoint point : SPAWN_STUMBLE_MAIN) {
               spawn(selectedStumble[stumbleClass], point.x, point.y, point.z, point.heading);
          }
          
          int devaStumbleClass = Rnd.get(1, 4);
          for (SpawnPoint point : SPAWN_DEVA_STUMBLE) {
               spawn(NPC_IDS_DEVA_STUMBLE[idx][devaStumbleClass - 1], point.x, point.y, point.z, point.heading);
          }
          
          int highType = Rnd.get(1, 3);
          int highClass = Rnd.get(1, 4);
          int[] selectedHigh = null;
          if (highType == 1) selectedHigh = NPC_IDS[idx];
          else if (highType == 2) selectedHigh = NPC_IDS_VRITRA[idx];
          else selectedHigh = NPC_IDS_TIAMAT[idx];
          
          for (SpawnPoint point : SPAWN_HIGH_MAIN) {
               spawn(selectedHigh[highClass - 1], point.x, point.y, point.z, point.heading);
          }
          
          int guardType = Rnd.get(1, 3);
          int guardClass = Rnd.get(1, 4);
          int[] selectedGuard = null;
          if (guardType == 1) selectedGuard = NPC_IDS_ERESH_GUARD[idx];
          else if (guardType == 2) selectedGuard = NPC_IDS_VRITRA_GUARD[idx];
          else selectedGuard = NPC_IDS_TIAMAT_GUARD[idx];
          
          for (SpawnPoint point : SPAWN_GUARD_MAIN) {
               spawn(selectedGuard[guardClass - 1], point.x, point.y, point.z, point.heading);
          }
          
          int doorMobType = Rnd.get(1, 3);
          spawn(NPC_IDS_DEVA_GUARD[idx][doorMobType], 510.39917f, 458.81943f, 322.0f, (byte)21);
     }
    
    private void IDTransformEreshWarp(int level) {
        int idx = getLevelIndexByLevel(level);
        int npcId = NPC_IDS_WARP[idx][Rnd.get(0, 3)];
        for (SpawnPoint point : SPAWN_WARP) {
            sp(npcId, point.x, point.y, point.z, point.heading, point.delay);
        }
    }
    
    private void IDTransformVritraWarp(int level) {
        int idx = getLevelIndexByLevel(level);
        int npcId = NPC_IDS_WARP[idx][4 + Rnd.get(0, 3)];
        for (SpawnPoint point : SPAWN_WARP) {
            sp(npcId, point.x, point.y, point.z, point.heading, point.delay);
        }
    }
    
    private void IDTransformTiamatWarp(int level) {
        int idx = getLevelIndexByLevel(level);
        int npcId = NPC_IDS_WARP[idx][8 + Rnd.get(0, 3)];
        for (SpawnPoint point : SPAWN_WARP) {
            sp(npcId, point.x, point.y, point.z, point.heading, point.delay);
        }
    }
    
    @Override
    public void onInstanceDestroy() {
        if (timerInstance != null) {
            timerInstance.cancel(false);
        }
        if (timerPrepare != null) {
            timerPrepare.cancel(false);
        }
        stopInstanceTask();
        isInstanceDestroyed = true;
        instanceReward.clear();
        doors.clear();
    }
    
    private void stopInstanceTask() {
        for (FastList.Node<Future<?>> n = oblivionTask.head(), end = oblivionTask.tail(); (n = n.getNext()) != end; ) {
            if (n.getValue() != null) {
                n.getValue().cancel(true);
            }
        }
    }
    
    protected void despawnNpc(Npc npc) {
        if (npc != null) {
            npc.getController().onDelete();
        }
    }
    
    private void deleteNpc(int npcId) {
        if (getNpc(npcId) != null) {
            getNpc(npcId).getController().onDelete();
        }
    }
    
    protected void killNpc(List<Npc> npcs) {
        for (Npc npc: npcs) {
            npc.getController().die();
        }
    }
    
    protected List<Npc> getNpcs(int npcId) {
        if (!isInstanceDestroyed) {
            return instance.getNpcs(npcId);
        }
        return null;
    }
    
    @Override
    public void onPlayerLogOut(Player player) {
        removeEffects(player);
    }
    
    @Override
    public void onLeaveInstance(Player player) {
        removeEffects(player);
    }
    
    private void removeEffects(Player player) {
        PlayerEffectController effectController = player.getEffectController();
        effectController.removeEffect(4808);
        effectController.removeEffect(4813);
        effectController.removeEffect(4818);
        effectController.removeEffect(4824);
        effectController.removeEffect(4831);
        effectController.removeEffect(4834);
        effectController.removeEffect(4835);
        effectController.removeEffect(4836);
    }
    
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time) {
        sp(npcId, x, y, z, h, 0, time, 0, null);
    }
    
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final int msg, final Race race) {
        sp(npcId, x, y, z, h, 0, time, msg, race);
    }
    
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int entityId, final int time, final int msg, final Race race) {
        oblivionTask.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    spawn(npcId, x, y, z, h, entityId);
                    if (msg > 0) {
                        sendMsgByRace(msg, race, 0);
                    }
                }
            }
        }, time));
    }
    
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final String walkerId) {
        oblivionTask.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    Npc npc = (Npc) spawn(npcId, x, y, z, h);
                    npc.getSpawn().setWalkerId(walkerId);
                    WalkManager.startWalking((NpcAI2) npc.getAi2());
                }
            }
        }, time));
    }
    
    private void sendMsg(final String str) {
        instance.doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player player) {
                PacketSendUtility.sendWhiteMessageOnCenter(player, str);
            }
        });
    }
    
    protected void sendMsgByRace(final int msg, final Race race, int time) {
        ThreadPoolManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                instance.doOnAllPlayers(new Visitor<Player>() {
                    @Override
                    public void visit(Player player) {
                        if (player.getRace().equals(race) || race.equals(Race.PC_ALL)) {
                            PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msg));
                        }
                    }
                });
            }
        }, time);
    }
}