package be.bluexin.ticworkaround;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;
import slimeknights.tconstruct.library.Util;
import slimeknights.tconstruct.library.tinkering.TinkersItem;
import slimeknights.tconstruct.library.utils.ToolHelper;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Mod(
        modid = Ticworkaround.MOD_ID,
        name = Ticworkaround.MOD_NAME,
        version = Ticworkaround.VERSION,
        dependencies = "required-after:" + Util.MODID
)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Ticworkaround {

    public static final String MOD_ID = "ticworkaround";
    public static final String MOD_NAME = "Ticworkaround";
    public static final String VERSION = "1.0-SNAPSHOT";

    @NetworkCheckHandler
    public boolean acceptRemotes(Map<String, String> unused, Side unused_) {
        return true;
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent e) {
        e.registerServerCommand(new CommandBase() {
            @Override
            public String getName() {
                return "ticfix";
            }

            @Override
            public String getUsage(ICommandSender sender) {
                return "/ticfix [optional target] to repair, /ticfix break [optional target]  to break. Target defaults to command sender.";
            }

            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
                boolean br = args.length > 0 && args[0].equals("break");
                EntityPlayerMP target = args.length > (br ? 1 : 0) ? getPlayer(server, sender, args[br ? 1 : 0]) : getCommandSenderAsPlayer(sender);
                ItemStack is = /*ToolHelper.*/playerIsHoldingItemWith(target, iss -> !iss.isEmpty() && iss.getItem() instanceof TinkersItem);
                if (is.isEmpty()) {
                    sender.sendMessage(new TextComponentString(target.getDisplayNameString() + " has no tool in hand."));
                } else if (br) {
                    if (!ToolHelper.isBroken(is)) {
                        ToolHelper.breakTool(is, target);
                        sender.sendMessage(new TextComponentString("Broke " + target.getDisplayNameString() + "'s " + is.getDisplayName() + "."));
                        target.sendMessage(new TextComponentString("Your " + is.getDisplayName() + " has been broken."));
                    } else {
                        sender.sendMessage(new TextComponentString("Broke " + target.getDisplayNameString() + "'s " + is.getDisplayName() + " is already broken."));
                    }
                } else {
                    if (ToolHelper.isBroken(is) && is.getItemDamage() == is.getMaxDamage()) {
                        ToolHelper.repairTool(is, is.getMaxDamage());
                        sender.sendMessage(new TextComponentString("Repaired " + target.getDisplayNameString() + "'s " + is.getDisplayName() + "."));
                        target.sendMessage(new TextComponentString("Your " + is.getDisplayName() + " has been repaired."));
                    } else {
                        sender.sendMessage(new TextComponentString(target.getDisplayNameString() + "'s " + is.getDisplayName() + " is not broken while at max durability."));
                    }
                }


            }

            @Override
            public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
                if (args.length == 1) {
                    String s = args[args.length - 1];
                    List<String> list = Lists.newArrayList();

                    if (doesStringStartWith(s, "break")) list.add("break");
                    for (GameProfile gameprofile : server.getOnlinePlayerProfiles()) {
                        if (doesStringStartWith(s, gameprofile.getName())) {
                            list.add(gameprofile.getName());
                        }
                    }

                    return list;
                } else {
                    return Collections.<String>emptyList();
                }
            }
        });
    }

    /* From TiC's ToolHelper in newer versions */
    private static ItemStack playerIsHoldingItemWith(EntityPlayer player, Predicate<ItemStack> predicate) {
        ItemStack tool = player.getHeldItemMainhand();
        if (!predicate.test(tool)) {
            tool = player.getHeldItemOffhand();
            if (!predicate.test(tool)) {
                return ItemStack.EMPTY;
            }
        }
        return tool;
    }
}
