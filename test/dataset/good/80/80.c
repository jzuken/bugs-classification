int main(int argc, char* argv[])
{
	correct_input(argc, argv);
	fopen_s(&file_in, argv[1], "rb");
	fopen_s(&file_out, argv[3], "wb");

	struct bmh_file file_header;
	struct bmh_info info_header;
	fread(&file_header, sizeof(file_header), 1, file_in);
	fread(&info_header, sizeof(info_header), 1, file_in);

	unsigned char* bitmap_image = (unsigned char*)(malloc(info_header.size_image));
	fseek(file_in, file_header.bf_off_bits, SEEK_SET);
	fread(bitmap_image, 1, file_header.bf_size, file_in);
	
	filters(bitmap_image, info_header.height, info_header.widht, argv[2]);
	
	fwrite(&file_header, sizeof(file_header), 1, file_out);
	fwrite(&info_header, sizeof(info_header), 1, file_out);
	for (int i = 0; i < info_header.size_image; i++)
	{
		fwrite(&bitmap_image[i], 1, 1, file_out);

	}

	free(bitmap_image);
	fclose(file_in);
	fclose(file_out);
	return 0;
}