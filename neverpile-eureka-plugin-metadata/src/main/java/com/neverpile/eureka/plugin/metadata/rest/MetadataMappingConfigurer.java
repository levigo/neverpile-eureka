package com.neverpile.eureka.plugin.metadata.rest;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;
import org.springframework.stereotype.Component;

import com.neverpile.eureka.plugin.metadata.service.Metadata;
import com.neverpile.eureka.plugin.metadata.service.MetadataElement;
import com.neverpile.eureka.rest.configuration.ModelMapperConfiguration.ModelMapperConfigurer;


@Component
public class MetadataMappingConfigurer implements ModelMapperConfigurer {
  @Override
  public void configure(final ModelMapper mapper) {
    mapper
      .createTypeMap(MetadataDto.class, Metadata.class)
      .setPostConverter(new Converter<MetadataDto, Metadata>() {
        @Override
        public Metadata convert(final MappingContext<MetadataDto, Metadata> context) {
          context.getSource().getElements().forEach((k,v) -> context.getDestination().put(k, mapper.map(v, MetadataElement.class)));
          return null;
        }
      });
    
    mapper
    .createTypeMap(Metadata.class, MetadataDto.class)
    .setPostConverter(new Converter<Metadata, MetadataDto>() {
      @Override
      public MetadataDto convert(final MappingContext<Metadata, MetadataDto> context) {
        context.getSource().forEach((k,v) -> context.getDestination().set(k, mapper.map(v, MetadataElementDto.class)));
        return null;
      }
    });
  }
}
