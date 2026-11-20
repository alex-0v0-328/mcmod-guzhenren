package net.alex.guzhenren.item;

/** 书册物品（{@link BookletItem}）承载的书文：单页书册（{@link Booklet}）或分节古籍（{@link Chronicle}）。 */

public sealed interface BookletText permits Booklet, Chronicle {}
