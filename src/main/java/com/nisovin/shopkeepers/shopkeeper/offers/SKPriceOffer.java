package com.nisovin.shopkeepers.shopkeeper.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Validate;

public class SKPriceOffer implements PriceOffer {

	private final ItemStack item; // not null/empty
	private final int price; // > 0

	public SKPriceOffer(ItemStack item, int price) {
		Validate.isTrue(!ItemUtils.isEmpty(item), "Item cannot be empty!");
		Validate.isTrue(price > 0, "Price has to be positive!");
		this.item = item.clone();
		this.price = price;
	}

	@Override
	public ItemStack getItem() {
		return item.clone();
	}

	@Override
	public int getPrice() {
		return price;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SKPriceOffer [item=");
		builder.append(item);
		builder.append(", price=");
		builder.append(price);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + item.hashCode();
		result = prime * result + price;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof SKPriceOffer)) return false;
		SKPriceOffer other = (SKPriceOffer) obj;
		if (!item.equals(other.item)) return false;
		if (price != other.price) return false;
		return true;
	}

	// //////////
	// STATIC UTILITIES
	// //////////

	public static void saveToConfig(ConfigurationSection config, String node, Collection<? extends PriceOffer> offers) {
		ConfigurationSection offersSection = config.createSection(node);
		int id = 1;
		for (PriceOffer offer : offers) {
			ItemStack item = offer.getItem(); // is a clone
			ConfigurationSection offerSection = offersSection.createSection(String.valueOf(id));
			offerSection.set("item", item);
			offerSection.set("price", offer.getPrice());
			id++;
		}
	}

	public static List<SKPriceOffer> loadFromConfig(ConfigurationSection config, String node, String errorContext) {
		List<SKPriceOffer> offers = new ArrayList<>();
		ConfigurationSection offersSection = config.getConfigurationSection(node);
		if (offersSection != null) {
			for (String id : offersSection.getKeys(false)) {
				ConfigurationSection offerSection = offersSection.getConfigurationSection(id);
				if (offerSection == null) continue; // invalid offer: not a section
				ItemStack item = offerSection.getItemStack("item");
				int price = offerSection.getInt("price");
				if (ItemUtils.isEmpty(item)) {
					// invalid offer
					Log.warning(StringUtils.prefix(errorContext, ": ", "Invalid price offer for " + id + ": item is empty"));
					continue;
				}
				if (price <= 0) {
					// invalid offer
					Log.warning(StringUtils.prefix(errorContext, ": ", "Invalid price offer for " + id + ": price has to be positive but is " + price));
					continue;
				}
				offers.add(new SKPriceOffer(item, price));
			}
		}
		return offers;
	}

	// Note: Returns the same list instance if no items were migrated.
	public static List<SKPriceOffer> migrateItems(List<SKPriceOffer> offers, String errorContext) {
		if (offers == null) return null;
		List<SKPriceOffer> migratedOffers = null;
		final int size = offers.size();
		for (int i = 0; i < size; ++i) {
			SKPriceOffer offer = offers.get(i);
			if (offer == null) continue; // skip invalid entries

			boolean itemsMigrated = false;
			boolean migrationFailed = false;

			ItemStack item = offer.getItem(); // note: is a clone
			ItemStack migratedItem = ItemUtils.migrateItemStack(item);
			if (!ItemUtils.isSimilar(item, migratedItem)) {
				if (ItemUtils.isEmpty(migratedItem) && !ItemUtils.isEmpty(item)) {
					migrationFailed = true;
				}
				item = migratedItem;
				itemsMigrated = true;
			}

			if (itemsMigrated) {
				if (migratedOffers == null) {
					migratedOffers = new ArrayList<>(size);
					for (int j = 0; j < i; ++j) {
						SKPriceOffer oldOffer = offers.get(j);
						if (oldOffer == null) continue; // skip invalid entries
						migratedOffers.add(oldOffer);
					}
				}

				if (migrationFailed) {
					Log.warning(StringUtils.prefix(errorContext, ": ", "Trading offer item migration failed for offer "
							+ (i + 1) + ": " + offer.toString()));
					continue; // skip this offer
				}
				assert !ItemUtils.isEmpty(item);
				migratedOffers.add(new SKPriceOffer(item, offer.getPrice()));
			}
		}
		return (migratedOffers == null) ? offers : migratedOffers;
	}
}
