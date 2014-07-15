package io.core9.server.undertow;

import io.core9.plugin.server.request.FileUpload;

public class FileUploadImpl implements FileUpload {
	
	private String filename;
	private String contentType;
	private String filepath;

	@Override
	public String getFilename() {
		return this.filename;
	}

	@Override
	public String getContentType() {
		return this.contentType;
	}

	@Override
	public String getFilepath() {
		return this.filepath;
	}
	
	public FileUploadImpl(String name, String contenttype, String filepath) {
		this.filename = name;
		this.contentType = contenttype;
		this.filepath = filepath;
	}

}
