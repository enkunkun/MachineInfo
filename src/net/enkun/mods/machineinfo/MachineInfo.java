package net.enkun.mods.machineinfo;

import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.logging.Level;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.Property;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import org.lwjgl.input.Keyboard;

import buildcraft.api.power.PowerHandler;
import buildcraft.api.transport.IPipeTile;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/*
 * MachineInfo
 *
 * 鉄エンジンの情報表示。視点に合わさっていれば表示する方法 (それだと思いっきりWaila) か、チャットログに表示する方法にしようと思う。
 * 見たいのはEnergyとLiquid。
 * 
 * クライアントへのTileEntity同期はなされていないようなアレであまり訳に立たないのでPacket。
 * Packetはカスタムパケットか。バニラのパケット送って情報取れれば一番いいのだけど。
 * 
 * 今のところPipeには非対応。一番必要な気もするがわからない。
 * 
 * TODO IC2
 * 
 */
@Mod(modid = "MachineInfo", name = "MachineInfo", version = "0.1dev")
//@NetworkMod
public class MachineInfo extends KeyHandler
{
	public static final String[] _ = {"//クソコードのかたまり"};
	private static final String LINE_SEPARATOR_CHAT = "\n";
	private int infokey = Keyboard.KEY_F;
	static KeyBinding keyBinding = new KeyBinding("MachineInfo", Keyboard.KEY_F);

	public MachineInfo()
	{
		super(new KeyBinding[] {keyBinding}, new boolean[] {false});
	}

	@Override
	public void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd, boolean isRepeat) {
		Minecraft mc = FMLClientHandler.instance().getClient();
		boolean hasWorld = mc.theWorld != null;
		boolean openedGui = mc.currentScreen != null;
		boolean hasBlock = mc.objectMouseOver != null;
		
		int x = mc.objectMouseOver.blockX;
		int y = mc.objectMouseOver.blockY;
		int z = mc.objectMouseOver.blockZ;
		TileEntity tile = mc.theWorld.getBlockTileEntity(x, y, z);
		
		if (tickEnd && hasWorld && !openedGui && hasBlock && tile != null) {
			
			String s = "";
			
			//fluid
			if (tile instanceof IFluidHandler) {
				//TODO Fluid Pipe
				FluidTankInfo[] tankInfo = ((IFluidHandler)tile).getTankInfo(ForgeDirection.UNKNOWN);
				if (tankInfo != null) {
					int id = mc.theWorld.getBlockId(x, y, z);
					int meta = mc.theWorld.getBlockMetadata(x, y, z);
					ItemStack drop = Block.blocksList[id].getBlockDropped(mc.theWorld, x, y, z, meta, 0).get(0);
					s += drop.getDisplayName() + LINE_SEPARATOR_CHAT;
					for (FluidTankInfo tank : tankInfo) {
						NumberFormat nf = NumberFormat.getInstance();
						String max = nf.format(tank.capacity);
						String current = nf.format(tank.fluid == null ? 0 : tank.fluid.amount);
						String name = tank.fluid == null ? "Empty" : tank.fluid.getFluid() == null ? "Empty" : tank.fluid.getFluid().getName();
						String locname = tank.fluid == null ? "Empty" : tank.fluid.getFluid() == null ? "Empty" : tank.fluid.getFluid().getLocalizedName(); //fluid.hogehoge
						
						s += String.format("%s %s/%s mB", name, current, max) + LINE_SEPARATOR_CHAT;
					}
				}
			}

			//power
			//うまくいってないです
			//TODO Pipe
			PowerHandler powerHandler = null;
			
			Class clazz = tile.getClass();
			if (clazz.getName().equals("buildcraft.transport.TileGenericPipe")) {
				clazz = ((IPipeTile) tile).getPipe().getClass();
			}
			
			while (clazz != null) {
				Field[] fields = clazz.getDeclaredFields();
				for (Field field : fields) {
					System.out.println(field.getType().getName());
					if (field.getType().getName().equals("buildcraft.api.power.PowerHandler")) {
						field.setAccessible(true);
						try {
							powerHandler = (PowerHandler) field.get(tile);
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
					}
				}
				if (powerHandler == null) {
					clazz = clazz.getSuperclass();
				} else {
					break;
				}
			}
			System.out.println("powerHandler: " + (powerHandler != null));
			
			if (powerHandler != null) {
				float f = powerHandler.getEnergyStored();
				float g = powerHandler.getMaxEnergyStored();
				s += f + " / " + g + " MJ";
				
			}
			
			mc.thePlayer.addChatMessage(s);
		}
	}

	@Override
	public void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd) {
	}

	@Override
	public String getLabel() {
		return "MachineInfo";
	}
	
	@Override
	public EnumSet<TickType> ticks() {
		return EnumSet.of(TickType.CLIENT);
	}

	@EventHandler
	public void preinit(FMLPreInitializationEvent event)
	{
		Configuration cfg = new Configuration(event.getSuggestedConfigurationFile());
		try {
			Property prop = cfg.get(Configuration.CATEGORY_GENERAL, "InformationKey", infokey, ("machine information display key" + Configuration.NEW_LINE + "Key codes: http://minecraft.gamepedia.com/Key_Codes"));
			infokey = prop.getInt();
		} catch (Exception e) {
			FMLLog.log(Level.SEVERE, e, "Configuration Error");
		} finally {
			if (cfg.hasChanged()) {
				cfg.save();
			}
		}
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		KeyBindingRegistry.registerKeyBinding(this);
	}

}
