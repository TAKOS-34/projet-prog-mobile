declare namespace Express {
    namespace Multer {
        interface File {
            buffer: Buffer;
            mimetype: string;
        }
    }
}