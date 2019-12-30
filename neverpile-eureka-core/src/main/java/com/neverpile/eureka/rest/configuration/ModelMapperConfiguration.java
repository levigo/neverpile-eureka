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
        dto.setDocumentId(source.getDocumentId());
        dto.setVersionTimestamp(source.getVersionTimestamp());

        ArrayList<ContentElementDto> contentElementDtos = new ArrayList<>();
        if (source.getContentElements() != null) {
          for (ContentElement ce : source.getContentElements()) {
            contentElementDtos.add(m.map(ce, ContentElementDto.class));
          }
        }
        dto.setFacet("contentElements", contentElementDtos);

        dto.setFacet("dateCreated", source.getDateCreated());
        dto.setFacet("dateModified", source.getDateModified());
        return dto;
      }
    };

    Converter<DocumentDto, Document> dtoToDocConv = new AbstractConverter<DocumentDto, Document>() {
      @SuppressWarnings("unchecked")
      @Override
      protected Document convert(final DocumentDto source) {
        Document doc = new Document();
        doc.setDocumentId(source.getDocumentId());
        doc.setVersionTimestamp(source.getVersionTimestamp());

        ArrayList<ContentElement> contentElements = new ArrayList<>();
        if (source.getFacets().get("contentElements") != null) {
          for (ContentElementDto dto : (List<ContentElementDto>) source.getFacets().get("contentElements")) {
            contentElements.add(m.map(dto, ContentElement.class));
          }
        }
        doc.setContentElements(contentElements);

        doc.setDateCreated((Instant) source.getFacets().get("dateCreated"));
        doc.setDateModified((Instant) source.getFacets().get("dateModified"));
        return doc;
      }
    };

    m.addConverter(docToDtoConv);
    m.addConverter(dtoToDocConv);

    if(null != configurers)
      configurers.forEach(c -> c.configure(m));

    return m;
  }
}
