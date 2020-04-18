package de.MineBug.schematics;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.scheduler.BukkitRunnable;

import de.MineBug.schematics.jnbt.ByteArrayTag;
import de.MineBug.schematics.jnbt.ByteTag;
import de.MineBug.schematics.jnbt.CompoundTag;
import de.MineBug.schematics.jnbt.DoubleTag;
import de.MineBug.schematics.jnbt.FloatTag;
import de.MineBug.schematics.jnbt.IntTag;
import de.MineBug.schematics.jnbt.ListTag;
import de.MineBug.schematics.jnbt.LongTag;
import de.MineBug.schematics.jnbt.NBTInputStream;
import de.MineBug.schematics.jnbt.ShortTag;
import de.MineBug.schematics.jnbt.StringTag;
import de.MineBug.schematics.jnbt.Tag;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.TileEntity;


public class SchematicManager {

	public static void pasteSchematic(Location loc, Schematic schematic) {
		short[] blocks = schematic.getBlocks();
		byte[] blockData = schematic.getData();

		short length = schematic.getLenght();
		short width = schematic.getWidth();
		short height = schematic.getHeight();
		
		BlockOffset offset = schematic.getOffset();
		
		loc.setX(loc.getX() + offset.getX());
		loc.setY(loc.getY() + offset.getY());
		loc.setZ(loc.getZ() + offset.getZ());
		
		new BukkitRunnable() {
			int i = 0;
			@Override
			public void run() {
				for (int y = 0; y < height; ++y) {
					for (int z = 0; z < length; ++z) {
						int index = y * width * length + z * width + i;
						Block block = new Location(loc.getWorld(), i + loc.getX(), y + loc.getY(), z + loc.getZ()).getBlock();
						block.setTypeIdAndData(blocks[index], blockData[index], true);
					}
				}
				if (!(i < width-1)) {
					
					new BukkitRunnable() {
						
						@Override
						public void run() {
							for (Tag t : schematic.getTileentities()) {
								CompoundTag ct = (CompoundTag) t;
								Map<String, Tag> tiledata = ct.getValue();
								int x = getChildTag(tiledata, "x", IntTag.class).getValue() + (int)loc.getX();
								int y = getChildTag(tiledata, "y", IntTag.class).getValue() + (int)loc.getY();
								int z = getChildTag(tiledata, "z", IntTag.class).getValue() + (int)loc.getZ();
								CraftWorld ws = (CraftWorld) loc.getWorld();
								TileEntity te = ws.getHandle().getTileEntity(new BlockPosition(x-1, y, z));
								
								if (te != null) {
									NBTTagCompound ntc = new NBTTagCompound();
									
									for (String tagkey : tiledata.keySet()) {
										Class<? extends Tag> clazz = tiledata.get(tagkey).getClass();
										if(clazz.equals(ByteArrayTag.class)) {
											ntc.setByteArray(tagkey, getChildTag(tiledata, tagkey, ByteArrayTag.class).getValue());
										} else if(clazz.equals(ByteTag.class)) {
											ntc.setByte(tagkey, getChildTag(tiledata, tagkey, ByteTag.class).getValue());
										} else if(clazz.equals(DoubleTag.class)) {
											ntc.setDouble(tagkey, getChildTag(tiledata, tagkey, DoubleTag.class).getValue());
										} else if(clazz.equals(FloatTag.class)) {
											ntc.setFloat(tagkey, getChildTag(tiledata, tagkey, FloatTag.class).getValue());
										} else if(clazz.equals(IntTag.class)) {
											ntc.setInt(tagkey, getChildTag(tiledata, tagkey, IntTag.class).getValue());
										} else if(clazz.equals(LongTag.class)) {
											ntc.setLong(tagkey, getChildTag(tiledata, tagkey, LongTag.class).getValue());
										} else if(clazz.equals(ShortTag.class)) {
											ntc.setShort(tagkey, getChildTag(tiledata, tagkey, ShortTag.class).getValue());
										} else if(clazz.equals(StringTag.class)) {
											ntc.setString(tagkey, getChildTag(tiledata, tagkey, StringTag.class).getValue());
										} else {Bukkit.getConsoleSender().sendMessage("Invalid Tag on " + x + " - " + y + " - " + z);}
									}

									ntc.setInt("x", (x-1));
									ntc.setInt("y", y);
									ntc.setInt("z", z);
									
									te.a(ntc);
									te.update();
									
									ws.getHandle().setTileEntity(te.getPosition(), te);
								}
							}
						}
					}.runTaskLater(SchematicPaster.getInstance(), 10);
					
					this.cancel();
				} else {
					i ++;
				}
			}
		}.runTaskTimer(SchematicPaster.getInstance(), 10, 1);
	}
	
    @SuppressWarnings("resource")
	public static Schematic loadSchematic(File file) {
    	try {
            FileInputStream stream = new FileInputStream(file);
            NBTInputStream nbtStream = new NBTInputStream(new GZIPInputStream(stream));
            
            CompoundTag schematicTag = (CompoundTag) nbtStream.readTag();
            if (!schematicTag.getName().equals("Schematic")) {
                throw new IllegalArgumentException("Tag \"Schematic\" does not exist or is not first");
            }
     
            Map<String, Tag> schematic = schematicTag.getValue();
            if (!schematic.containsKey("Blocks")) {
                throw new IllegalArgumentException("Schematic file is missing a \"Blocks\" tag");
            }
     
            short width = getChildTag(schematic, "Width", ShortTag.class).getValue();
            short length = getChildTag(schematic, "Length", ShortTag.class).getValue();
            short height = getChildTag(schematic, "Height", ShortTag.class).getValue();
            
            byte[] blockId = getChildTag(schematic, "Blocks", ByteArrayTag.class).getValue();
            byte[] blockData = getChildTag(schematic, "Data", ByteArrayTag.class).getValue();
            byte[] addId = new byte[0];
            short[] blocks = new short[blockId.length];

            BlockOffset offset = new BlockOffset(0, 0, 0);
            if (schematic.containsKey("WEOffsetX") && schematic.containsKey("WEOffsetY") && schematic.containsKey("WEOffsetZ")) {
            	int offsetX = getChildTag(schematic, "WEOffsetX", IntTag.class).getValue();
                int offsetY = getChildTag(schematic, "WEOffsetY", IntTag.class).getValue();
                int offsetZ = getChildTag(schematic, "WEOffsetZ", IntTag.class).getValue();
                offset = new BlockOffset(offsetX, offsetY, offsetZ);
            }
            
			List<Tag> tileentities =  getChildTag(schematic, "TileEntities", ListTag.class).getValue();
            
            if (schematic.containsKey("AddBlocks")) {
                addId = getChildTag(schematic, "AddBlocks", ByteArrayTag.class).getValue();
            }
     
            for (int index = 0; index < blockId.length; index++) {
                if ((index >> 1) >= addId.length) { 
                    blocks[index] = (short) (blockId[index] & 0xFF);
                } else {
                    if ((index & 1) == 0) {
                        blocks[index] = (short) (((addId[index >> 1] & 0x0F) << 8) + (blockId[index] & 0xFF));
                    } else {
                        blocks[index] = (short) (((addId[index >> 1] & 0xF0) << 4) + (blockId[index] & 0xFF));
                    }
                }
            }
     
            return new Schematic(blocks, blockData, tileentities, width, length, height, offset);
    	} catch (IOException ex) {
    		ex.printStackTrace();
    	}
    	return null;
    }

	private static <T extends Tag> T getChildTag(Map<String, Tag> items, String key, Class<T> expected)
			throws IllegalArgumentException {
		if (!items.containsKey(key)) {
			throw new IllegalArgumentException("Schematic file is missing a \"" + key + "\" tag");
		}
		Tag tag = items.get(key);
		if (!expected.isInstance(tag)) {
			throw new IllegalArgumentException(key + " tag is not of tag type " + expected.getName());
		}
		
		return expected.cast(tag);
	}
	
}

class BlockOffset {
	
	private int x,y,z;
	
	public BlockOffset(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getZ() {
		return z;
	}
	
}