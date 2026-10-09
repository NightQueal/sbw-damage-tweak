package civilized.sbw;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
public final class AttributedDamageSource extends DamageSource {
 private final DamageSource original;
 public final double inputMultiplier;
 public AttributedDamageSource(DamageSource original,Entity projectile,double multiplier) {
  super(original.typeHolder(),projectile,attacker(projectile));this.original=original;this.inputMultiplier=multiplier;
 }
 private static Entity attacker(Entity e) {return e instanceof Projectile p && p.getOwner()!=null?p.getOwner():e;}
 @Override public boolean is(TagKey<DamageType> tag) {return original.is(tag);}
}
