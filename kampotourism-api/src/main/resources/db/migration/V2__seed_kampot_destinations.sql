-- Seed content taken from the frontend demo (the six real Kampot destinations).
-- TODO(content): image_url values are temporary hotlinks to the demo site; move
--   images to your own storage/CDN. latitude/longitude, entrance fees and opening
--   hours are intentionally NULL until someone verifies them.

insert into destination (slug, category, image_url, featured, sort_order) values
 ('bokor',          'NATURE', 'https://testingdesign.lovable.app/assets/bokor-wH3XHjGH.jpg',       true,  1),
 ('riverside',      'TOWN',   'https://testingdesign.lovable.app/assets/riverside-mLdh4aDi.jpg',   true,  2),
 ('salt-fields',    'NATURE', 'https://testingdesign.lovable.app/assets/salt-DvJt3hnO.jpg',        true,  3),
 ('pepper-country', 'FOOD',   'https://testingdesign.lovable.app/assets/pepper-DdsNUrBJ.jpg',      false, 4),
 ('mangroves',      'WATER',  'https://testingdesign.lovable.app/assets/kayak-gUsgPmh9.jpg',       false, 5),
 ('river-sunrise',  'WATER',  'https://testingdesign.lovable.app/assets/hero-fDX2qTrU.jpg',        false, 6);

insert into destination_translation
    (destination_id, lang, name, tag, summary, lead, description, highlights, tips,
     distance, duration, best_for, bring)
select d.id, 'en', v.name, v.tag, v.summary, v.lead,
       v.description::jsonb, v.highlights::jsonb, v.tips::jsonb,
       v.distance, v.duration, v.best_for, v.bring
from (values
 ('bokor',
  $t$Bokor National Park$t$,
  $t$Mountains & mist$t$,
  $t$Trade tropical heat for cloud-wrapped hills and sweeping highland views.$t$,
  $t$Rising above Kampot, Bokor is where the air cools, the forest thickens and the views stretch all the way to the sea.$t$,
  $j$["The winding road up the mountain passes rainforest, waterfalls and the weathered remains of a 1920s hill station.","Come early for clearer skies, bring a light layer and leave time to stop at the viewpoints on the way down."]$j$,
  $j$[{"title":"The old hill station","text":"Faded colonial buildings wrapped in cloud and quiet."},{"title":"Popokvil waterfall","text":"A tiered cascade, best after the rains."},{"title":"The highland viewpoints","text":"Look out over the coast, islands and Kampot below."}]$j$,
  $j$["Leave by 8am to beat the clouds","Rent a scooter or hire a driver for the day","Weather changes fast — pack a rain layer"]$j$,
  $t$40 km from town$t$, $t$A full day$t$, $t$Views, cool air, history$t$, $t$Jacket, water, cash$t$),

 ('riverside',
  $t$Kampot riverside$t$,
  $t$Slow days & local life$t$,
  $t$Pastel shophouses, shaded streets and evenings that unfold by the water.$t$,
  $t$The heart of Kampot beats along the Praek Tuek Chhu, where the old town meets the river.$t$,
  $j$["Wander French-era shophouses, stop for iced coffee, and join locals on the promenade as the light turns gold.","In the evening, small boats drift upriver for sunset and fireflies."]$j$,
  $j$[{"title":"Old market","text":"Bustling stalls by day, a food hub by night."},{"title":"Sunset cruises","text":"Gentle boat trips toward the mountains."},{"title":"Riverside cafés","text":"Long lunches with a view of the water."}]$j$,
  $j$["Walk in the late afternoon when it's cooler","Book a firefly boat after dark","Try the local pepper crab"]$j$,
  $t$In town$t$, $t$Half a day$t$, $t$Strolls, cafés, sunsets$t$, $t$Comfortable shoes$t$),

 ('salt-fields',
  $t$The salt fields$t$,
  $t$Landscapes & traditions$t$,
  $t$A working landscape of mirrored skies, salt crystals and golden light.$t$,
  $t$Flat pans of seawater stretch toward the horizon, reflecting the sky and a centuries-old way of working.$t$,
  $j$["From January to May, families harvest salt by hand, carrying baskets along narrow dykes.","Visit at dusk, when the fields glow and become perfect mirrors."]$j$,
  $j$[{"title":"Harvest season","text":"See the work up close from January to May."},{"title":"Golden hour","text":"Some of the best light in the province."},{"title":"Salt museum","text":"Learn how the craft has shaped local life."}]$j$,
  $j$["Ask before photographing workers","Stay on the dykes and paths","Go late afternoon for the light"]$j$,
  $t$5 km from town$t$, $t$2–3 hours$t$, $t$Photography, culture$t$, $t$Hat, sunscreen$t$),

 ('pepper-country',
  $t$Kampot pepper country$t$,
  $t$Grown here$t$,
  $t$Red earth roads, climbing vines and the world's most celebrated peppercorns.$t$,
  $t$Kampot pepper has protected origin status — and the farms that grow it welcome curious visitors.$t$,
  $j$["Walk between the vines, learn how green, black, red and white pepper are made, and taste each one.","Many farms serve lunch with views of the limestone hills."]$j$,
  $j$[{"title":"Farm tours","text":"Free guided walks at many family farms."},{"title":"Tastings","text":"Four peppers, four very different flavours."},{"title":"Lunch with a view","text":"Fresh dishes cooked with the harvest."}]$j$,
  $j$["Buy directly from farms to support growers","Combine with the Secret Lake nearby","Harvest runs February to May"]$j$,
  $t$15–25 km$t$, $t$Half a day$t$, $t$Food lovers$t$, $t$Appetite, cash$t$),

 ('mangroves',
  $t$Paddle the mangroves$t$,
  $t$On the water$t$,
  $t$Quiet waterways, green reflections and a different view of river life.$t$,
  $t$Upriver, the banks close in and palms arch overhead — the best way through is by paddle.$t$,
  $j$["Rent a kayak or SUP and drift through tunnels of nipa palm.","Mornings are calm and cool; local guides can show you hidden channels."]$j$,
  $j$[{"title":"Palm tunnels","text":"Shady channels with endless reflections."},{"title":"Birdlife","text":"Kingfishers and herons along the banks."},{"title":"Riverside stops","text":"Cool off at a guesthouse swimming deck."}]$j$,
  $j$["Go early before the wind picks up","Check tide times","Wear reef-safe sunscreen"]$j$,
  $t$Upriver$t$, $t$2–3 hours$t$, $t$Active travellers$t$, $t$Swimwear, dry bag$t$),

 ('river-sunrise',
  $t$River at first light$t$,
  $t$Slow mornings$t$,
  $t$Mist on the water, fishing boats heading out and the mountain glowing pink.$t$,
  $t$Kampot is at its most magical before the town wakes — when the river is still and the mountain catches first light.$t$,
  $j$["Find a quiet spot on the riverbank, or take a dawn boat with a local fisherman.","Follow it with breakfast at the market."]$j$,
  $j$[{"title":"Fishing boats","text":"Watch the day's work begin on the water."},{"title":"Mountain glow","text":"Bokor turns gold above the mist."},{"title":"Market breakfast","text":"Noodle soup and fresh coffee."}]$j$,
  $j$["Sunrise is around 6am","Bring a light layer","Ask your guesthouse about dawn boats"]$j$,
  $t$In town$t$, $t$1–2 hours$t$, $t$Early risers$t$, $t$Camera$t$)
) as v(slug, name, tag, summary, lead, description, highlights, tips,
       distance, duration, best_for, bring)
join destination d on d.slug = v.slug;
