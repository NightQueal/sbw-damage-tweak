package civilized.sbw;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
public final class AttributedDamageSource extends DamageSource {
 private final DamageSource original;
 public final double inputMultiplier;
 public final boolean blast;
 public final net.minecraft.world.phys.Vec3 impactOrigin;
 public AttributedDamageSource(DamageSource original,Entity projectile,double multiplier) {
  this(original,projectile,multiplier,false,projectile.position());
 }
 public AttributedDamageSource(DamageSource original,Entity projectile,double multiplier,boolean blast,net.minecraft.world.phys.Vec3 origin) {
  super(original.typeHolder(),projectile,attacker(projectile));this.original=original;this.inputMultiplier=multiplier;this.blast=blast;this.impactOrigin=origin;
 }
 private static Entity attacker(Entity e) {return e instanceof Projectile p && p.getOwner()!=null?p.getOwner():e;}
 @Override public boolean is(TagKey<DamageType> tag) {return original.is(tag);}
}
