DROP INDEX IF EXISTS "Like_postId_userId_anonymousUserId_key";

CREATE UNIQUE INDEX "Like_postId_userId_key"
ON "Like" ("postId", "userId")
WHERE "userId" IS NOT NULL;

CREATE UNIQUE INDEX "Like_postId_anonymousUserId_key"
ON "Like" ("postId", "anonymousUserId")
WHERE "anonymousUserId" IS NOT NULL;



DROP INDEX IF EXISTS "CommentLike_commentId_userId_anonymousUserId_key";

CREATE UNIQUE INDEX "CommentLike_commentId_userId_key"
ON "CommentLike" ("commentId", "userId")
WHERE "userId" IS NOT NULL;

CREATE UNIQUE INDEX "CommentLike_commentId_anonymousUserId_key"
ON "CommentLike" ("commentId", "anonymousUserId")
WHERE "anonymousUserId" IS NOT NULL;