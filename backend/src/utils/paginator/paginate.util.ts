export function getNextCursor<T extends string | number>(items: { id: T }[], limit: number): T | undefined {
    return items.length > limit ? items[limit - 1]?.id : undefined;
}