import { createParamDecorator, ExecutionContext } from '@nestjs/common';

export const GetGroup = createParamDecorator((data: unknown, ctx: ExecutionContext) => {
    const request = ctx.switchToHttp().getRequest();
    return request.group;
});