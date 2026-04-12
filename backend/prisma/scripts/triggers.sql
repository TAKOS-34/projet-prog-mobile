CREATE OR REPLACE FUNCTION update_member_count() RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        UPDATE "Group" SET "nbMembers" = "nbMembers" + 1 WHERE id = NEW."groupId";
        UPDATE "User" SET "nbGroups" = "nbGroups" + 1 WHERE id = NEW."userId";
    ELSIF (TG_OP = 'DELETE') THEN
        UPDATE "Group" SET "nbMembers" = "nbMembers" - 1 WHERE id = OLD."groupId";
        UPDATE "User" SET "nbGroups" = "nbGroups" - 1 WHERE id = OLD."userId";
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_member_count ON "Member";
CREATE TRIGGER tr_member_count AFTER INSERT OR DELETE ON "Member" FOR EACH ROW EXECUTE FUNCTION update_member_count();



CREATE OR REPLACE FUNCTION update_post_count() RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        UPDATE "User" SET "nbPosts" = "nbPosts" + 1 WHERE id = NEW."userId";
        IF NEW."groupId" IS NOT NULL THEN
            UPDATE "Group" SET "nbPosts" = "nbPosts" + 1 WHERE id = NEW."groupId";
        END IF;
    ELSIF (TG_OP = 'DELETE') THEN
        UPDATE "User" SET "nbPosts" = "nbPosts" - 1 WHERE id = OLD."userId";
        IF OLD."groupId" IS NOT NULL THEN
            UPDATE "Group" SET "nbPosts" = "nbPosts" - 1 WHERE id = OLD."groupId";
        END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_post_count ON "Post";
CREATE TRIGGER tr_post_count AFTER INSERT OR DELETE ON "Post" FOR EACH ROW EXECUTE FUNCTION update_post_count();



CREATE OR REPLACE FUNCTION update_post_like_count() RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        UPDATE "Post" SET "nbLikes" = "nbLikes" + 1 WHERE id = NEW."postId";
    ELSIF (TG_OP = 'DELETE') THEN
        UPDATE "Post" SET "nbLikes" = "nbLikes" - 1 WHERE id = OLD."postId";
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_post_like_count ON "Like";
CREATE TRIGGER tr_post_like_count AFTER INSERT OR DELETE ON "Like" FOR EACH ROW EXECUTE FUNCTION update_post_like_count();



CREATE OR REPLACE FUNCTION update_post_comment_count() RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        UPDATE "Post" SET "nbComments" = "nbComments" + 1 WHERE id = NEW."postId";
    ELSIF (TG_OP = 'DELETE') THEN
        UPDATE "Post" SET "nbComments" = "nbComments" - 1 WHERE id = OLD."postId";
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_post_comment_count ON "Comment";
CREATE TRIGGER tr_post_comment_count AFTER INSERT OR DELETE ON "Comment" FOR EACH ROW EXECUTE FUNCTION update_post_comment_count();



CREATE OR REPLACE FUNCTION update_comment_like_count() RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        UPDATE "Comment" SET "nbLikes" = "nbLikes" + 1 WHERE id = NEW."commentId";
    ELSIF (TG_OP = 'DELETE') THEN
        UPDATE "Comment" SET "nbLikes" = "nbLikes" - 1 WHERE id = OLD."commentId";
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_comment_like_count ON "CommentLike";
CREATE TRIGGER tr_comment_like_count AFTER INSERT OR DELETE ON "CommentLike" FOR EACH ROW EXECUTE FUNCTION update_comment_like_count();



CREATE OR REPLACE FUNCTION update_comment_reply_count() RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        IF NEW."parentId" IS NOT NULL THEN
            UPDATE "Comment" SET "nbReplies" = "nbReplies" + 1 WHERE id = NEW."parentId";
        END IF;
    ELSIF (TG_OP = 'DELETE') THEN
        IF OLD."parentId" IS NOT NULL THEN
            UPDATE "Comment" SET "nbReplies" = "nbReplies" - 1 WHERE id = OLD."parentId";
        END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_comment_reply_count ON "Comment";
CREATE TRIGGER tr_comment_reply_count AFTER INSERT OR DELETE ON "Comment" FOR EACH ROW EXECUTE FUNCTION update_comment_reply_count();



CREATE OR REPLACE FUNCTION update_tag_usage_count() RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        UPDATE "Tag" SET "nbUses" = "nbUses" + 1 WHERE id = NEW."tagId";
    ELSIF (TG_OP = 'DELETE') THEN
        UPDATE "Tag" SET "nbUses" = "nbUses" - 1 WHERE id = OLD."tagId";
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_tag_usage_count ON "PostTag";
CREATE TRIGGER tr_tag_usage_count AFTER INSERT OR DELETE ON "PostTag" FOR EACH ROW EXECUTE FUNCTION update_tag_usage_count();