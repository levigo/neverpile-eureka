package com.neverpile.eureka.rest.configuration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.neverpile.eureka.impl.documentservice.DocumentPdo;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.api.document.content.ContentElementDto;


@Configuration
public class ModelMapperConfiguration {
  public interface ModelMapperConfigurer {
    void configure(ModelMapper mapper);
  }

  @Autowired(required = false)
  List<ModelMapperConfigurer> configurers;

  @Bean
  @Qualifier("document")
  ModelMapper documentMapper() {
    ModelMapper m = new ModelMapper();

    Converter<Document, DocumentDto> docToDtoConv = new AbstractConverter<Document, DocumentDto>() {
      @Override
      protected DocumentDto convert(final Document source) {
        DocumentDto dto = new DocumentDto();
        docToDto(source, dto, m);
        return dto;
      }
    };

    Converter<DocumentDto, Document> dtoToDocConv = new AbstractConverter<DocumentDto, Document>() {
      @SuppressWarnings("unchecked")
      @Override
      protected Document convert(final DocumentDto source) {
        Document doc = new Document();
        dtoToDoc(source, doc, m);
        return doc;
      }
    };

    Converter<DocumentPdo, DocumentDto> pdoToDtoConv = new AbstractConverter<DocumentPdo, DocumentDto>() {
      @Override
      protected DocumentDto convert(final DocumentPdo source) {
        DocumentDto dto = new DocumentDto();
        docToDto(source, dto, m);
        return dto;
      }
    };

    Converter<DocumentDto, DocumentPdo> dtoToPdoConv = new AbstractConverter<DocumentDto, DocumentPdo>() {
      @SuppressWarnings("unchecked")
      @Override
      protected DocumentPdo convert(final DocumentDto source) {
        DocumentPdo doc = new DocumentPdo();
        dtoToDoc(source, doc, m);
        return doc;
      }
    };

    m.addConverter(docToDtoConv);
    m.addConverter(dtoToDocConv);
    m.addConverter(pdoToDtoConv);
    m.addConverter(dtoToPdoConv);

    if (null != configurers)
      configurers.forEach(c -> c.configure(m));

    return m;
  }

  private void dtoToDoc(DocumentDto source, Document doc, ModelMapper m) {
    doc.setDocumentId(source.getDocumentId());
    doc.setVersionTimestamp(source.getVersionTimestamp());

    ArrayList<ContentElement> contentElemnts = new ArrayList<>();
    if (source.getFacets().get("contentElements") != null) {
      for (ContentElementDto dto : (List<ContentElementDto>) source.getFacets().get("contentElements")) {
        contentElemnts.add(m.map(dto, ContentElement.class));
      }
    }
    doc.setContentElements(contentElemnts);

    doc.setDateCreated((Instant) source.getFacets().get("dateCreated"));
    doc.setDateModified((Instant) source.getFacets().get("dateModified"));
  }

  private void docToDto(Document source, DocumentDto dto, ModelMapper m) {
    dto.setDocumentId(source.getDocumentId());
    dto.setVersionTimestamp(source.getVersionTimestamp());

    ArrayList<ContentElementDto> contentElemntDtos = new ArrayList<>();
    if (source.getContentElements() != null) {
      for (ContentElement ce : source.getContentElements()) {
        contentElemntDtos.add(m.map(ce, ContentElementDto.class));
      }
    }
    dto.setFacet("contentElements", contentElemntDtos);

    dto.setFacet("dateCreated", source.getDateCreated());
    dto.setFacet("dateModified", source.getDateModified());
  }
}
