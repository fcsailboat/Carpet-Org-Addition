package boat.carpetorgaddition.periodic;

import boat.carpetorgaddition.dialog.DialogProvider;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerResidents;
import boat.carpetorgaddition.periodic.fakeplayer.PlayerSerializationManager;
import boat.carpetorgaddition.periodic.parcel.ParcelManager;
import boat.carpetorgaddition.periodic.task.ServerTaskManager;
import boat.carpetorgaddition.rule.CustomRuleValueManager;
import boat.carpetorgaddition.rule.RuleConfig;
import boat.carpetorgaddition.wheel.inventory.FabricPlayerAccessManager;
import boat.carpetorgaddition.wheel.misc.LibrarianVillagerPoiCache;
import boat.carpetorgaddition.wheel.page.PageManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ServerComponentCoordinator {
    static {
        // 注册服务器保存事件
        ServerLifecycleEvents.AFTER_SAVE.register((server, _, _) -> ServerComponentCoordinator.of(server).onServerSave());
    }

    private final MinecraftServer server;
    /**
     * 快递管理器
     */
    private final ParcelManager parcelManager;
    /**
     * 服务器任务管理器
     */
    private final ServerTaskManager serverTaskManager = new ServerTaskManager();
    /**
     * 翻页管理器
     */
    private final PageManager pageManager;
    private final CustomRuleValueManager customRuleValueManager;
    private final PlayerSerializationManager playerSerializationManager;
    private final FabricPlayerAccessManager accessManager;
    private final RuleConfig ruleConfig;
    private final DialogProvider dialogProvider;
    private final FakePlayerResidents fakePlayerResidents;
    private final LibrarianVillagerPoiCache librarianVillagerPoiCache;
    public static final ScopedValue<MinecraftServer> SERVER_INSTANCE = ScopedValue.newInstance();

    private ServerComponentCoordinator(MinecraftServer server) {
        this.server = server;
        this.parcelManager = new ParcelManager(server);
        this.pageManager = new PageManager(server);
        this.customRuleValueManager = new CustomRuleValueManager(server);
        this.playerSerializationManager = new PlayerSerializationManager(server);
        this.accessManager = new FabricPlayerAccessManager(server);
        this.ruleConfig = new RuleConfig(server);
        this.dialogProvider = new DialogProvider(server);
        this.fakePlayerResidents = new FakePlayerResidents(server);
        this.librarianVillagerPoiCache = new LibrarianVillagerPoiCache();
    }

    public static ServerComponentCoordinator of(MinecraftServer server) {
        PeriodicTaskManagerInterface instance = (PeriodicTaskManagerInterface) server;
        ServerComponentCoordinator coordinator = instance.carpet_Org_Addition$getServerComponentCoordinator();
        if (coordinator == null) {
            coordinator = new ServerComponentCoordinator(server);
            instance.carpet_Org_Addition$setServerComponentCoordinator(coordinator);
        }
        return coordinator;
    }

    /**
     * 在服务器启动时调用
     */
    public void onServerStarted() {
        this.playerSerializationManager.init();
        this.fakePlayerResidents.cleanupFiles();
        this.customRuleValueManager.load();
        this.playerSerializationManager.autoLogin();
    }

    public void tick() {
        this.parcelManager.tick();
        ServerTickRateManager tickManager = this.server.tickRateManager();
        this.serverTaskManager.tick(tickManager);
        this.pageManager.tick();
        this.accessManager.tick();
    }

    public ParcelManager getParcelManager() {
        return this.parcelManager;
    }

    public ServerTaskManager getServerTaskManager() {
        return this.serverTaskManager;
    }

    public CustomRuleValueManager getCustomRuleValueManager() {
        return this.customRuleValueManager;
    }

    public PageManager getPageManager() {
        return this.pageManager;
    }

    public PlayerSerializationManager getPlayerSerializationManager() {
        return this.playerSerializationManager;
    }

    public FabricPlayerAccessManager getAccessManager() {
        return accessManager;
    }

    public RuleConfig getRuleConfig() {
        return this.ruleConfig;
    }

    public DialogProvider getDialogProvider() {
        return this.dialogProvider;
    }

    public FakePlayerResidents getSavedFakePlayer() {
        return this.fakePlayerResidents;
    }

    public LibrarianVillagerPoiCache getLibrarianVillagerPoiCache() {
        return this.librarianVillagerPoiCache;
    }

    private void onServerSave() {
        this.customRuleValueManager.onServerSave();
        this.playerSerializationManager.onServerSave();
        this.fakePlayerResidents.onServerSave();
    }
}
