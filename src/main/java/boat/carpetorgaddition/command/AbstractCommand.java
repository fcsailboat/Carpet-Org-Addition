package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.config.GlobalConfigs;

public abstract class AbstractCommand {
    public final void register() {
        String[] commands = this.getCustomNames();
        if (commands.length < 1) {
            CarpetOrgAddition.LOGGER.warn(
                    // 译：服务端/客户端命令[/%s]无法使用自定义名称注册，正在使用默认名称.
                    "{} command [/{}] cannot be registered with a custom name. The default name is being used instead.",
                    this.getEnvironment(),
                    this.getDefaultName()
            );
            this.register(this.getDefaultName());
        } else {
            for (String name : commands) {
                this.register(name);
            }
        }
    }

    /**
     * @return 命令的环境
     */
    protected abstract String getEnvironment();

    /**
     * 注册命令
     *
     * @param name 命令的名称
     */
    public abstract void register(String name);

    /**
     * 命令的名称，可以有多个，表示使用不同的名称多次注册，如果为0个，则在注册时会自动使用默认名称
     */
    public String[] getCustomNames() {
        return GlobalConfigs.getCommand(this.getDefaultName());
    }

    public String getAvailableName() {
        return this.getCustomNames()[0];
    }

    public abstract String getDefaultName();

    public boolean shouldRegister() {
        return true;
    }
}
