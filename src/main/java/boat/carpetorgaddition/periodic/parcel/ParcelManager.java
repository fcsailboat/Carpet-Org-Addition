package boat.carpetorgaddition.periodic.parcel;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.command.MailCommand;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.Counter;
import boat.carpetorgaddition.wheel.WorldFormat;
import boat.carpetorgaddition.wheel.page.PageManager;
import boat.carpetorgaddition.wheel.page.PagedCollection;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 快递管理器
 */
public class ParcelManager {
    private final TreeSet<Parcel> parcels = new TreeSet<>();
    private final WorldFormat worldFormat;
    private final MinecraftServer server;

    public ParcelManager(MinecraftServer server) {
        this.server = server;
        this.worldFormat = new WorldFormat(server, Parcel.EXPRESS);
        this.init();
    }

    private void init() {
        // 从文件读取快递信息
        for (File file : this.worldFormat.listFiles(WorldFormat.NBT_EXTENSIONS)) {
            String name = IOUtils.getFileNameWithoutExtension(file);
            OptionalInt optional = MathUtils.tryParseInt(name);
            if (optional.isEmpty()) {
                continue;
            }
            int id = optional.getAsInt();
            CompoundTag nbt;
            try {
                nbt = NbtIo.read(file.toPath());
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.warn("Failed to read cached data from file {}", file, e);
                continue;
            }
            if (nbt == null) {
                continue;
            }
            Parcel parcel;
            try {
                parcel = Parcel.readNbt(this.server, nbt, id);
            } catch (RuntimeException e) {
                CarpetOrgAddition.LOGGER.warn("Unable to load parcel data: ", e);
                continue;
            }
            // 快递对象物品为空，删除对应的文件
            if (parcel.isComplete()) {
                parcel.delete();
                continue;
            }
            this.parcels.add(parcel);
        }
    }

    /**
     * 提示玩家接收快递
     */
    public void promptToCollect(ServerPlayer player) {
        if (player instanceof EntityPlayerMPFake) {
            return;
        }
        List<Parcel> list = this.parcels.stream()
                .filter(parcel -> parcel.isRecipient(player))
                .filter(parcel -> !parcel.isRecall())
                .toList();
        if (list.isEmpty()) {
            return;
        }
        ArrayList<Supplier<Component>> messages = new ArrayList<>();
        for (Parcel parcel : list) {
            messages.add(() -> {
                Component clickRun = TextProvider.clickRun(CommandProvider.collectParcel(parcel.getId(), false));
                return MailCommand.KEY.then("prompt_collect").translate(parcel.getCount(), parcel.getDisplayName(), clickRun);
            });
        }
        PageManager pageManager = ServerComponentCoordinator.getCoordinator(this.server).getPageManager();
        CommandSourceStack source = player.createCommandSourceStack();
        PagedCollection collection = pageManager.newPagedCollection(source);
        collection.addContent(messages);
        MessageUtils.sendEmptyMessage(source);
        CommandUtils.handlingException(collection::print, source);
    }

    /**
     * 每个游戏刻删除已经寄件完成的快递
     */
    public void tick() {
        this.parcels.removeIf(Parcel::isComplete);
    }

    /**
     * 添加新快递
     */
    public void put(Parcel parcel) throws IOException {
        if (parcel.isComplete()) {
            CarpetOrgAddition.LOGGER.warn("Attempted to send an empty item, ignored");
            return;
        }
        this.parcels.add(parcel);
        parcel.send();
        parcel.checkRecipientPermission();
        // 将快递信息写入本地文件
        parcel.save();
    }

    public Stream<Parcel> stream() {
        return this.parcels.stream();
    }

    /**
     * @return 获取快递发送者的快递被接收者接收的消息
     */
    public static Component getReceiveNotice(ServerPlayer player, Counter<Item> counter) {
        ArrayList<Component> list = new ArrayList<>();
        for (Item item : counter) {
            list.add(TextBuilder.combineAll(ServerUtils.getName(item), "*", counter.getCount(item)));
        }
        TextBuilder builder = TextBuilder.of(MailCommand.NOTICE.then("collect").translate(player.getDisplayName()));
        builder.setGrayItalic();
        builder.setHover(TextBuilder.joinList(list));
        return builder.build();
    }

    /**
     * 使用二分查找获取指定单号的快递
     *
     * @param id 要查找的快递单号
     */
    public Optional<Parcel> binarySearch(int id) {
        List<Parcel> list = this.parcels.stream().toList();
        int left = 0;
        int right = list.size() - 1;
        while (left <= right) {
            int mid = (left + right) >>> 1;
            int currentId = list.get(mid).getId();
            if (currentId < id) {
                left = mid + 1;
            } else if (currentId > id) {
                right = mid - 1;
            } else {
                return Optional.of(list.get(mid));
            }
        }
        return Optional.empty();
    }

    /**
     * 生成快递单号
     */
    public int generateNumber() {
        // 没有快递发出，快递单号为1
        if (this.parcels.isEmpty()) {
            return 1;
        }
        // 集合最后一个元素id等于集合长度，说明前面的单号都是连续的，新单号为集合长度+1
        if (this.parcels.last().getId() == this.parcels.size()) {
            return this.parcels.size() + 1;
        }
        // 遍历集合找到空缺的单号
        int number = 0;
        for (Parcel parcel : this.parcels) {
            number++;
            if (number == parcel.getId()) {
                continue;
            }
            return number;
        }
        throw new IllegalStateException();
    }
}
