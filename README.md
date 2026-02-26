# HypingAuctions

## Placeholders

Liste des placeholders pour HAuctions:

| Placeholder                                                                                              | Description                                                      |
|----------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| `%hauctions_name_<auctions/expired/bought/sales>_<index>%`                                               | name of the item                                                 |
| `%hauctions_price_<auctions/expired/bought/sales>_<index>%`                                              | the price of the item                                            |
| `%hauctions_material_<auctions/expired/bought/sales>_<index>%`                                           | material of the item                                             |
| `%hauctions_custom-model-data_<auctions/expired/bought/sales>_<index>%`                                  | custom model data of the item                                    |
| `%hauctions_quantity_<auctions/expired/bought/sales>_<index>%`                                           | quantity of the item                                             |
| `%hauctions_lore_<auctions/expired/bought/sales>_<index>%`                                               | lore of the item                                                 |
| `%hauctions_currency[_formatted]_<auctions/expired/bought/sales>_<index>%`                               | currency of the item                                             |
| `%hauctions_expiration_<auctions/expired/bought/sales>_<index>%`                                         | expiration date of the item                                      |
| `%hauctions_seller_<auctions/expired/bought/sales>_<index>%`                                             | seller of the item                                               |
| `%hauctions_exists_<auctions/expired/bought/sales>_<index>%`                                             | tell you if the item with the index `<index>` exists             |
| `%hauctions_page%`                                                                                       | actual page (will be removed)                                    |
| `%hauctions_pages%`                                                                                      | maximum page (will be removed)                                   |
| `%hauctions_menu%`                                                                                       | type of the actual menu (AUCTION, EXPIRED, BOUGHT, UNKNOWN)      |
| `%hauctions_limit%`                                                                                      | amount of items that the player can sell                         |
| `%hauctions_sales%`                                                                                      | amount of actual sales of the player                             |
| `%hauctions_taxes%`                                                                                      | percentage of taxes the player have to pay                       |
| `%hauctions_category%`                                                                                   | actual filtered category                                         |
| `%hauctions_banned_remaining_time[_formatted]%`                                                          | remaining banned time (-1 OR "Permanent" if perma ban)           |
| `%hauctions_banned%`                                                                                     | if the player is banned or not (true OR false)                   |
| `%hauctions_sales-id_<index>%`                                                                           | the sale id of the seller (can be used for the `expire` command) |
| `%hauctions_target_<name/lore/material/quantity/custom-model-data/price/currency/seller/expiration/tax/averageprice>%` | the properties of the targeted item per the buying command       |
|  `%hauctions_shulkerbox_[auctions/expired/bought/sales]_[index]%%`                                        | Returns true if the item at the specified index in the selected menu is a shulkerbox, otherwise false |

> `<var>`         - a variable
>
> `[optional]`    - an optional parameter


## Commandes

Liste des commandes pour HAuctions:

| Commande                                                                          | Description                                                                                                                                               |
|-----------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/hauctions open <menu>`                                                          | open the selected menu (menu can be auctions/bought/expired)                                                                                              |
| `/hauctions sell <price> [currency]`                                              | sell the hand item to the auctions house for the <price> and the <currency> or by default the selected one in the config                                  |
| `/hauctions buy <player> [id]`                                                    | buy the item for the player <player> the auctions is selected using the id in the context or if no id provided, we'll use the target                      |
| `/hauctions buying <player> <id>`                                                 | target the item with the <id> to use placeholders or the buy command without <id>                                                                         |
| `/hauctions expire <player> <id>`                                                 | expire the <id> item from the sales of the <player>                                                                                                       |
| `/hauctions page <next/previous/id>`                                              | change the context page of the player if there is multiple pages in the auction house                                                                     |
| `/hauctions player <player> <target>`                                             | set the player filter of the <player> to <target>                                                                                                         |
| `/hauctions refresh <player>`                                                     | refresh the player auction house context items and placeholders                                                                                           |
| `/hauctions reload`                                                               | reload the plugin configurations                                                                                                                          |
| `/hauctions search <player> <search/clear>`                                       | set the search filter criteria to the <search> or remove the filter if <clear>                                                                            |
| `/hauctions sort <player> <reset/category/price/date/name> [category] <asc/desc>` | set the sort filter criteria to the <category> or remove the filter if <reset> or sort by <price/date/name> with <asc/desc>                               |
| `/hauctions withdraw <player> <id>`                                               | withdraw the <id> item from the bought of the <player>                                                                                                    |
| `/hauctions ban <player> [time]`                                                  | ban the <player> for the <time> in seconds by default or specify the unit (s/min/h/d/w/mon/y). if no time provided, the player will be banned permanently |
| `/hauctions unban <player>`                                                       | unban the <player>                                                                                                                                        |
| `/hauctions shulkerinspect [target] [index]`                                      | Allows inspection of the contents of a shulkerbox listed in the auction house of player [target] at position [index]                                      |
| `/hauctions history [player]`                                                     | Get auction history in general, or for a specific player.                                     |

> `<var>`         - a variable
>
> `[optional]`    - an optional parameter

## Translation keys

The following translation keys are currently used by the plugin:

- `hyping.hypingauctions.command.avgtest.average-price`
- `hyping.hypingauctions.command.avgtest.cache-cleared`
- `hyping.hypingauctions.command.avgtest.calculating`
- `hyping.hypingauctions.command.avgtest.disabled`
- `hyping.hypingauctions.command.avgtest.enabled`
- `hyping.hypingauctions.command.avgtest.error`
- `hyping.hypingauctions.command.avgtest.must-hold-item`
- `hyping.hypingauctions.command.avgtest.no-permission`
- `hyping.hypingauctions.command.avgtest.no-price-data`
- `hyping.hypingauctions.command.avgtest.player-only`
- `hyping.hypingauctions.command.avgtest.usage`
- `hyping.hypingauctions.command.back.player-data-not-found`
- `hyping.hypingauctions.command.back.player-only`
- `hyping.hypingauctions.command.cache.clear.success`
- `hyping.hypingauctions.command.cache.info.details`
- `hyping.hypingauctions.command.cache.info.header`
- `hyping.hypingauctions.command.cache.invalidate.error`
- `hyping.hypingauctions.command.cache.invalidate.offline`
- `hyping.hypingauctions.command.cache.invalidate.online`
- `hyping.hypingauctions.command.cache.invalidate.player-not-found`
- `hyping.hypingauctions.command.cache.invalidate.uuid`
- `hyping.hypingauctions.command.cache.reload.error`
- `hyping.hypingauctions.command.cache.reload.success`
- `hyping.hypingauctions.command.cache.stats.error`
- `hyping.hypingauctions.command.cache.stats.logged`
- `hyping.hypingauctions.command.cache.usage.clear`
- `hyping.hypingauctions.command.cache.usage.header`
- `hyping.hypingauctions.command.cache.usage.info`
- `hyping.hypingauctions.command.cache.usage.invalidate`
- `hyping.hypingauctions.command.cache.usage.reload`
- `hyping.hypingauctions.command.cache.usage.stats`
- `hyping.hypingauctions.command.cachetest.cache-info`
- `hyping.hypingauctions.command.cachetest.functionality.cache-info`
- `hyping.hypingauctions.command.cachetest.functionality.clear`
- `hyping.hypingauctions.command.cachetest.functionality.clear-failed`
- `hyping.hypingauctions.command.cachetest.functionality.complete`
- `hyping.hypingauctions.command.cachetest.functionality.invalidation`
- `hyping.hypingauctions.command.cachetest.functionality.invalidation-failed`
- `hyping.hypingauctions.command.cachetest.functionality.null-handling`
- `hyping.hypingauctions.command.cachetest.functionality.reload`
- `hyping.hypingauctions.command.cachetest.functionality.reload-failed`
- `hyping.hypingauctions.command.cachetest.functionality.start`
- `hyping.hypingauctions.command.cachetest.no-offline-players`
- `hyping.hypingauctions.command.cachetest.performance.cached-hits`
- `hyping.hypingauctions.command.cachetest.performance.cached-misses`
- `hyping.hypingauctions.command.cachetest.performance.direct-calls`
- `hyping.hypingauctions.command.cachetest.performance.results`
- `hyping.hypingauctions.command.cachetest.performance.start`
- `hyping.hypingauctions.command.cachetest.result.fail`
- `hyping.hypingauctions.command.cachetest.result.pass`
- `hyping.hypingauctions.command.cachetest.stress.duration`
- `hyping.hypingauctions.command.cachetest.stress.failed`
- `hyping.hypingauctions.command.cachetest.stress.interrupted`
- `hyping.hypingauctions.command.cachetest.stress.passed`
- `hyping.hypingauctions.command.cachetest.stress.results`
- `hyping.hypingauctions.command.cachetest.stress.start`
- `hyping.hypingauctions.command.cachetest.stress.successful`
- `hyping.hypingauctions.command.cachetest.stress.thread-failed`
- `hyping.hypingauctions.command.cachetest.stress.threads`
- `hyping.hypingauctions.command.cachetest.usage.functionality`
- `hyping.hypingauctions.command.cachetest.usage.header`
- `hyping.hypingauctions.command.cachetest.usage.performance`
- `hyping.hypingauctions.command.cachetest.usage.stress`
- `hyping.hypingauctions.command.cancel.already-sold`
- `hyping.hypingauctions.command.cancel.disabled`
- `hyping.hypingauctions.command.cancel.nothing-to-cancel`
- `hyping.hypingauctions.command.cancel.success`
- `hyping.hypingauctions.command.cancel.window-expired`
- `hyping.hypingauctions.command.find.must-hold-item`
- `hyping.hypingauctions.command.find.player-only`
- `hyping.hypingauctions.command.help.decorator.left`
- `hyping.hypingauctions.command.help.decorator.right`
- `hyping.hypingauctions.command.help.entry.hover`
- `hyping.hypingauctions.command.help.entry.prefix`
- `hyping.hypingauctions.command.help.entry.separator`
- `hyping.hypingauctions.command.help.invalid-page`
- `hyping.hypingauctions.command.help.next`
- `hyping.hypingauctions.command.help.next.hover`
- `hyping.hypingauctions.command.help.page`
- `hyping.hypingauctions.command.help.previous`
- `hyping.hypingauctions.command.help.previous.hover`
- `hyping.hypingauctions.command.help.title`
- `hyping.hypingauctions.command.history.transaction-format-fallback`
- `hyping.hypingauctions.command.history.transaction-format-loaded`
- `hyping.hypingauctions.command.history.transaction-format-not-loaded`
- `hyping.hypingauctions.command.open.console-requires-target`
- `hyping.hypingauctions.command.open.no-permission`
- `hyping.hypingauctions.command.open.open-failed`
- `hyping.hypingauctions.command.open.open-failed-for-player`
- `hyping.hypingauctions.command.page.console-requires-target`
- `hyping.hypingauctions.command.page.no-permission`
- `hyping.hypingauctions.command.page.set`
- `hyping.hypingauctions.command.page.set-for-player`
- `hyping.hypingauctions.command.player.never-played`
- `hyping.hypingauctions.command.player.set`
- `hyping.hypingauctions.command.premium-buying.auction-not-found`
- `hyping.hypingauctions.command.premium-buying.invalid-slot`
- `hyping.hypingauctions.command.premium-buying.invalid-slot-range`
- `hyping.hypingauctions.command.premium-buying.success`
- `hyping.hypingauctions.command.premium-buying.target-offline`
- `hyping.hypingauctions.command.premium-buying.usage`
- `hyping.hypingauctions.command.premium-promote.cannot-promote`
- `hyping.hypingauctions.command.premium-promote.currency-not-found`
- `hyping.hypingauctions.command.premium-promote.no-item-selected`
- `hyping.hypingauctions.command.premium-promote.player-not-found`
- `hyping.hypingauctions.command.premium-promote.usage`
- `hyping.hypingauctions.command.premium-shulkerbox.auction-not-found`
- `hyping.hypingauctions.command.premium-shulkerbox.invalid-number`
- `hyping.hypingauctions.command.premium-shulkerbox.invalid-slot`
- `hyping.hypingauctions.command.premium-shulkerbox.not-shulkerbox`
- `hyping.hypingauctions.command.premium-shulkerbox.player-not-on-server`
- `hyping.hypingauctions.command.premium-target.auction-not-found`
- `hyping.hypingauctions.command.premium-target.invalid-index`
- `hyping.hypingauctions.command.premium-target.player-not-found`
- `hyping.hypingauctions.command.premium-target.success`
- `hyping.hypingauctions.command.premium-target.usage`
- `hyping.hypingauctions.command.premium.already-premium`
- `hyping.hypingauctions.command.premium.auction-not-found`
- `hyping.hypingauctions.command.premium.currency-not-found`
- `hyping.hypingauctions.command.premium.expired`
- `hyping.hypingauctions.command.premium.invalid-auction-id`
- `hyping.hypingauctions.command.premium.no-slots`
- `hyping.hypingauctions.command.premium.no-target`
- `hyping.hypingauctions.command.premium.player-not-found`
- `hyping.hypingauctions.command.premium.promotion-failed`
- `hyping.hypingauctions.command.premium.promotion-success`
- `hyping.hypingauctions.command.premium.usage`
- `hyping.hypingauctions.command.pricereset.all-cleared`
- `hyping.hypingauctions.command.pricereset.error`
- `hyping.hypingauctions.command.pricereset.info.all`
- `hyping.hypingauctions.command.pricereset.info.description`
- `hyping.hypingauctions.command.pricereset.info.hand`
- `hyping.hypingauctions.command.pricereset.info.header`
- `hyping.hypingauctions.command.pricereset.info.help`
- `hyping.hypingauctions.command.pricereset.info.material`
- `hyping.hypingauctions.command.pricereset.info.spacer`
- `hyping.hypingauctions.command.pricereset.info.vanilla`
- `hyping.hypingauctions.command.pricereset.invalid-material`
- `hyping.hypingauctions.command.pricereset.material-cleared`
- `hyping.hypingauctions.command.pricereset.must-hold-item`
- `hyping.hypingauctions.command.pricereset.no-price-data`
- `hyping.hypingauctions.command.pricereset.player-only`
- `hyping.hypingauctions.command.pricereset.recalculated`
- `hyping.hypingauctions.command.pricereset.recalculating`
- `hyping.hypingauctions.command.pricereset.usage.material`
- `hyping.hypingauctions.command.pricereset.vanilla-cleared`
- `hyping.hypingauctions.command.pricereset.vanilla-clearing`
- `hyping.hypingauctions.command.refresh.console-requires-target`
- `hyping.hypingauctions.command.refresh.no-open-menu`
- `hyping.hypingauctions.command.refresh.no-permission`
- `hyping.hypingauctions.command.refresh.player-data-not-found`
- `hyping.hypingauctions.command.refresh.player-not-found`
- `hyping.hypingauctions.command.refresh.self-success`
- `hyping.hypingauctions.command.refresh.target-no-context`
- `hyping.hypingauctions.command.refresh.target-success`
- `hyping.hypingauctions.command.reload.complete`
- `hyping.hypingauctions.command.reload.start`
- `hyping.hypingauctions.command.root.not-available`
- `hyping.hypingauctions.command.root.player-only`
- `hyping.hypingauctions.command.root.usage`
- `hyping.hypingauctions.command.search.player-only`
- `hyping.hypingauctions.command.shulkerbox.context-not-found`
- `hyping.hypingauctions.command.shulkerbox.invalid-index-range`
- `hyping.hypingauctions.command.shulkerbox.invalid-number`
- `hyping.hypingauctions.command.shulkerbox.not-shulkerbox`
- `hyping.hypingauctions.command.shulkerbox.player-not-on-server`
- `hyping.hypingauctions.command.shulkerbox.target-not-found`
- `hyping.hypingauctions.command.shulkerinspect.context-not-found`
- `hyping.hypingauctions.command.shulkerinspect.no-target`
- `hyping.hypingauctions.command.shulkerinspect.not-shulkerbox`
- `hyping.hypingauctions.command.shulkerinspect.player-not-found`
- `hyping.hypingauctions.command.shulkerinspect.player-not-on-server`
- `hyping.hypingauctions.command.sign-search-clear.cleared`
- `hyping.hypingauctions.command.sign-search-clear.error`
- `hyping.hypingauctions.command.sign-search-clear.never-played`
- `hyping.hypingauctions.command.sign-search-clear.no-parameter`
- `hyping.hypingauctions.command.sign-search-clear.target-offline`
- `hyping.hypingauctions.command.sign-search-clear.usage`
- `hyping.hypingauctions.command.sign-search-set.empty-term`
- `hyping.hypingauctions.command.sign-search-set.error`
- `hyping.hypingauctions.command.sign-search-set.never-played`
- `hyping.hypingauctions.command.sign-search-set.player-success`
- `hyping.hypingauctions.command.sign-search-set.sender-success`
- `hyping.hypingauctions.command.sign-search-set.target-offline`
- `hyping.hypingauctions.command.sign-search-set.usage`
- `hyping.hypingauctions.command.sign-search.disabled`
- `hyping.hypingauctions.command.sign-search.error`
- `hyping.hypingauctions.command.sign-search.gui-error`
- `hyping.hypingauctions.command.sign-search.no-search-term`
- `hyping.hypingauctions.command.sign-search.search-success`
- `hyping.hypingauctions.command.similar.open-failed`
- `hyping.hypingauctions.command.similar.player-only`
- `hyping.hypingauctions.command.similar.showing`
- `hyping.hypingauctions.command.sort.category-set`
- `hyping.hypingauctions.command.sort.invalid-category`
- `hyping.hypingauctions.command.sort.invalid-order`
- `hyping.hypingauctions.command.sort.invalid-sort-type`
- `hyping.hypingauctions.command.sort.missing-category`
- `hyping.hypingauctions.command.sort.missing-order`
- `hyping.hypingauctions.command.sort.player-not-found`
- `hyping.hypingauctions.command.sort.reset`
- `hyping.hypingauctions.command.sort.set`
- `hyping.hypingauctions.command.sort.target-no-context`
- `hyping.hypingauctions.command.testlimit.current-limit`
- `hyping.hypingauctions.command.testlimit.custom-found`
- `hyping.hypingauctions.command.testlimit.custom-message`
- `hyping.hypingauctions.command.testlimit.custom-missing`
- `hyping.hypingauctions.command.testlimit.default-message`
- `hyping.hypingauctions.command.testlimit.detail-line`
- `hyping.hypingauctions.command.testlimit.footer`
- `hyping.hypingauctions.command.testlimit.header`
- `hyping.hypingauctions.command.testlimit.player-only`
- `hyping.hypingauctions.command.testmessage.bought-loaded`
- `hyping.hypingauctions.command.testmessage.bought-raw`
- `hyping.hypingauctions.command.testmessage.bought-test`
- `hyping.hypingauctions.command.testmessage.reload.complete`
- `hyping.hypingauctions.command.testmessage.reload.start`
- `hyping.hypingauctions.command.testmessage.sold-loaded`
- `hyping.hypingauctions.command.testmessage.sold-raw`
- `hyping.hypingauctions.command.testmessage.sold-test`
- `hyping.hypingauctions.command.testmessage.testing`
- `hyping.hypingauctions.command.withdraw.auction-not-found`
- `hyping.hypingauctions.command.withdraw.invalid-id`
- `hyping.hypingauctions.command.withdraw.player-never-played`
- `hyping.hypingauctions.command.withdraw.player-not-connected`
- `hyping.hypingauctions.command.withdraw.self-inventory-full`
- `hyping.hypingauctions.command.withdraw.target-inventory-full`
- `hyping.hypingauctions.gui.confirmation.cancel`
- `hyping.hypingauctions.gui.confirmation.confirm`
- `hyping.hypingauctions.gui.confirmation.title`
- `hyping.hypingauctions.gui.history.title`
- `hyping.hypingauctions.gui.history.title.buys`
- `hyping.hypingauctions.gui.history.title.sells`
- `hyping.hypingauctions.gui.shulkerbox.close`
- `hyping.hypingauctions.gui.shulkerbox.title`
